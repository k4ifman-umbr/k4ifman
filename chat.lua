local chat = {}

local ui = NEW_UI_LIB
local main_section = ui.create_tab(false, Menu.Find("Miscellaneous"), "Other", "Chat Message")
main_section.ref:Icon("\u{e14b}")
main_section = main_section:create("Основное"):create("Сообщение в чат")

local enable = main_section:switch("Включить", false, "\u{f00c}")
local bind_all = main_section:bind("Клавиша для общего чата", nil, "\u{e0cb}"):link_to_ui_disable_condition(enable)
local bind_team = main_section:bind("Клавиша для командного чата", nil, "\u{e0cb}"):link_to_ui_disable_condition(enable)
local sms = main_section:input("Сообщение", nil, "\u{f11c}"):link_to_ui_disable_condition(enable)
local delay = main_section:slider("Задержка, мс", 0, 1000, 100, nil, "\u{f017}"):link_to_ui_disable_condition(enable)

local start_number = main_section:input("Начальное число", "", "\u{f11c}"):link_to_ui_disable_condition(enable)
local decrement_value = main_section:input("Вычитаемое значение (>0)", "", "\u{f11c}"):link_to_ui_disable_condition(enable)
local finish_text = main_section:input("Финальное сообщение", "заглушка", "\u{f11c}"):link_to_ui_disable_condition(enable)

-- state
local state = {
  current_number = nil,
  decrement = nil,
  cooldown = { say = 0, say_team = 0 }
}

local function now() return os.clock() end
local function ms_to_sec(ms) return (tonumber(ms) or 0) / 1000 end

local function sanitize(text)
  text = tostring(text or "")
  -- экранируем двойные кавычки и убираем символы перевода строки
  text = text:gsub('"', '\\"'):gsub('[\r\n]', ' ')
  return text
end

local function send(channel, text)
  local safe = sanitize(text)
  if channel == "say" then
    -- оборачиваем в кавычки, чтобы не было инъекции через ; и т.п.
    Engine.ExecuteCommand('say "' .. safe .. '"')
  else
    Engine.ExecuteCommand('say_team "' .. safe .. '"')
  end
end

local function reset_counter()
  state.current_number = nil
  state.decrement = nil
end

local function ensure_counter(channel)
  if state.current_number and state.decrement then return true end
  local s = tonumber(start_number())
  local d = tonumber(decrement_value())
  if not s or not d or d <= 0 then
    send(channel, "Ошибка: проверьте числа (start_number и decrement>0)")
    return false
  end
  -- Можно привести к целым, если так задумано:
  state.current_number = math.floor(s)
  state.decrement = math.floor(d)
  return true
end

local function tick_counter(channel)
  local cur, dec = state.current_number, state.decrement
  if cur >= dec then
    local next_val = cur - dec
    send(channel, tostring(cur) .. " - " .. tostring(dec) .. " = " .. tostring(next_val))
    state.current_number = next_val
  else
    send(channel, finish_text() ~= "" and finish_text() or "заглушка")
    reset_counter()
  end
end

local function can_fire(channel)
  local cd = state.cooldown[channel] or 0
  return now() > cd
end

local function set_cooldown(channel)
  state.cooldown[channel] = now() + ms_to_sec(delay())
end

local function handle_free_text(channel)
  local text = sms()
  if text and text ~= "" then
    send(channel, text)
    set_cooldown(channel)
    return true
  end
  return false
end

local function tick_channel(channel, bind_fn)
  if not bind_fn() or not can_fire(channel) then return end

  -- 1) если есть обычное сообщение — отправляем и выходим
  if handle_free_text(channel) then return end

  -- 2) иначе работаем как счётчик
  if not ensure_counter(channel) then return end
  tick_counter(channel)
  set_cooldown(channel)
end

-- Сброс при выключении
local was_enabled = false

function chat.OnUpdate()
  local enabled = enable()
  if not enabled then
    if was_enabled then
      reset_counter()
      state.cooldown.say, state.cooldown.say_team = 0, 0
    end
    was_enabled = false
    return
  end
  was_enabled = true

  tick_channel("say", bind_all)
  tick_channel("say_team", bind_team)
end

return chat
