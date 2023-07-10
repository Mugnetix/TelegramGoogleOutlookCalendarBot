import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import org.joda.time.LocalDate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GOTelegramBot extends TelegramLongPollingBot
{
    private static LocalDate CURRENT_CALENDAR_DATE   = LocalDate.now();
    public  static String   CURRENT_GOOGLE_CALENDAR  = "";
    public  static String   CURRENT_OUTLOOK_CALENDAR = "";
    public  static int      MESSAGE_STATE            = 0;
    public  static String   BOT_TOKEN                = "";

    @Override
    public String getBotUsername() {
        return "GObot";
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Проверка на наличие текста в сообщении от пользователя
        if (update.hasMessage() && update.getMessage().hasText())
        {
            SendMessage message = new SendMessage();
            long chat_id = update.getMessage().getChatId();
            Main.chat_ID = chat_id;
            String message_text = update.getMessage().getText();

            if (message_text.equals("/events"))
            {
                if (CURRENT_OUTLOOK_CALENDAR.equals("") && CURRENT_GOOGLE_CALENDAR.equals(""))
                {
                    message.setChatId(chat_id);
                    message.setText("Нет установленных Outlook или Google календарей!\n\n" +
                            "Введите команду /setgoogle или /setoutlook для установки");
                }
                else
                {
                    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rowsInline = new EventKeyboardFactory().generateKeyboard(LocalDate.now());
                    markupInline.setKeyboard(rowsInline);
                    message.setReplyMarkup(markupInline);
                    message.setChatId(chat_id);
                    message.setText("Выберите дату для просмотра:");
                    MESSAGE_STATE = 0;
                }
            }
            else if (message_text.equals("/help") || message_text.equals("/start"))
            {
                MESSAGE_STATE = 0;
                message.setChatId(chat_id);
                message.setText("Команды в приватных чатах и группах:\n\n/events — Просмотр событий из текущих календарей Google и Outlook"
                        + "\n\n/setgoogle — Изменить текущий Google календарь"
                        + "\n\n/setoutlook — Изменить текущий Outlook календарь"
                        + "\n\n/listcalendars — Показать текущие Google и Outlook календари"
                        + "\n\n/notifs — Включить\\Отключить уведомления из текущих календарей. В чат или группу, " +
                            "из которой вызвана команда, будут отправляться уведомления о событиях за 30 мин. до их начала"
                        + "\n\nКоманды в каналах:"
                        + "\n\n/notifs — Включить\\Отключить уведомления из текущих календарей");
            }
            else if (message_text.contains("/setgoogle"))
            {
                message.setChatId(chat_id);
                message.setText("Введите идентификатор Google календаря");
                MESSAGE_STATE = 1;
            }
            else if (message_text.equals("/setoutlook"))
            {
                message.setChatId(chat_id);
                message.setText("Введите ICS ссылку на Outlook календарь");
                MESSAGE_STATE = 2;
            }
            else if (message_text.equals("/listcalendars"))
            {
                MESSAGE_STATE = 0;
                message.setChatId(chat_id);
                message.setText("Идентификатор Google календаря:\n" + CURRENT_GOOGLE_CALENDAR + "\n\nOutlook календарь:\n" + CURRENT_OUTLOOK_CALENDAR);
            }
            else if (message_text.equals("/notifs"))
            {
                if (CURRENT_OUTLOOK_CALENDAR.equals("") && CURRENT_GOOGLE_CALENDAR.equals(""))
                {
                    message.setChatId(chat_id);
                    message.setText("Нет установленных Outlook или Google календарей!\n\n" +
                            "Введите команду /setgoogle или /setoutlook для установки");
                }
                else
                {
                    MESSAGE_STATE = 0;
                    message.setChatId(chat_id);
                    boolean idInArray = false;
                    for (int i = 0; i < Main.notification_chat_IDs.size(); i++) {
                        if (Main.notification_chat_IDs.get(i) == chat_id) {
                            Main.notification_chat_IDs.remove(i);
                            message.setText("Уведомления из календарей отключены");
                            idInArray = true;
                        }
                    }
                    if (!idInArray) {
                        Main.notification_chat_IDs.add(chat_id);
                        message.setText("Уведомления из календарей включены");
                    }

                    try {
                        writeToFile("notif_id.txt", Main.notification_chat_IDs);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            else
            {
                if (MESSAGE_STATE == 1)
                {
                    message.setChatId(chat_id);
                    if (GoogleUtil.isValidGoogleId(message_text))
                    {
                        CURRENT_GOOGLE_CALENDAR = message_text;
                        message.setText("Календарь установлен");
                        List<String> calInfo = new ArrayList<>();
                        calInfo.add("google="+CURRENT_GOOGLE_CALENDAR);
                        calInfo.add("outlook="+CURRENT_OUTLOOK_CALENDAR);
                        try
                        {
                            writeToFile("calendar_config.txt", calInfo);
                        }
                        catch (IOException e) {System.out.println(e);}
                    }
                    else
                        message.setText("Введён неверный идентификатор, либо календарь с этим идентификатором недоступен!");
                    MESSAGE_STATE = 0;
                }
                else if (MESSAGE_STATE == 2)
                {
                    message.setChatId(chat_id);
                    if (OutlookUtil.downloadOutlookCal(message_text))
                    {
                        message.setText("Календарь установлен");
                        CURRENT_OUTLOOK_CALENDAR = message_text;
                        List<String> calInfo = new ArrayList<>();
                        calInfo.add("google="+CURRENT_GOOGLE_CALENDAR);
                        calInfo.add("outlook="+CURRENT_OUTLOOK_CALENDAR);
                        try
                        {
                            writeToFile("calendar_config.txt", calInfo);
                        }
                        catch (IOException e) {System.out.println(e);}
                    }
                    else
                        message.setText("Введена неверная ICS ссылка, либо календарь по этой ссылке недоступен!");
                    MESSAGE_STATE = 0;
                }
                else
                {
                    message.setChatId(chat_id);
                    message.setText(message_text);
                }
            }
            /*
                Ведение лога в консоль обо всех взаимодействиях бот<->пользователь:
                Данные пользователя, сообщение пользователя, ответ бота, время.
            */
            //log(user_first_name, user_last_name, Long.toString(user_id), message_text, message.getText(), user_username);

            try
            {
                execute(message); // Отправка пользователю результирующего сообщения после обработки
            }
            catch (TelegramApiException e) {e.printStackTrace();}
        }
        else if (update.hasChannelPost() && update.getChannelPost().hasText())
        {
            SendMessage channelMessage = new SendMessage();
            long channel_id = update.getChannelPost().getChatId();
            channelMessage.setChatId(channel_id);

            if (CURRENT_OUTLOOK_CALENDAR.equals("") && CURRENT_GOOGLE_CALENDAR.equals(""))
            {
                channelMessage.setText("Нет установленных Outlook или Google календарей!\n\n" +
                        "Настройте бота в личном чате.");
            }
            else {
                if (update.getChannelPost().getText().equals("/notifs")) {

                    boolean idInArray = false;
                    for (int i = 0; i < Main.notification_chat_IDs.size(); i++) {
                        if (Main.notification_chat_IDs.get(i) == channel_id) {
                            Main.notification_chat_IDs.remove(i);
                            channelMessage.setText("Уведомления из Google календаря для канала отключены");
                            idInArray = true;
                        }
                    }
                    if (!idInArray) {
                        Main.notification_chat_IDs.add(channel_id);
                        channelMessage.setText("Уведомления из Google календаря для канала включены");
                    }

                    try {
                        writeToFile("notif_id.txt", Main.notification_chat_IDs);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            try
            {
                execute(channelMessage); // Отправка результирующего сообщения после обработки
            }
            catch (TelegramApiException e) {e.printStackTrace();}
        }
        else if (update.hasCallbackQuery()) // Прослушивание callbackData
        {
            String call_data = update.getCallbackQuery().getData();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();
            long message_id = update.getCallbackQuery().getMessage().getMessageId();
            String inl_message_id = update.getCallbackQuery().getInlineMessageId();
            SendMessage message = new SendMessage();

            if (call_data.contains("eBtn")) // Содержит ли callbackData нужный префикс
            {
                message.setChatId(chat_id);
                String eventString = "";
                if(call_data.charAt(4) == '@')
                {
                    PropertyList outlookEvent = OutlookUtil.getOutlookEvent("outlookCal.ics", call_data.substring(5));

                    Property property = (Property) outlookEvent.getProperty("SUMMARY");
                    byte[] bytes = property.getValue().getBytes(UTF_8);
                    String value = new String(bytes, UTF_8);
                    eventString += value;

                    property = (Property) outlookEvent.getProperty("DTSTART");
                    String s = property.getValue();
                    eventString += "\nВ "
                            + s.substring(9, 11) + ":"
                            + s.substring(11, 13);

                    property = (Property) outlookEvent.getProperty("LOCATION");
                    bytes = property.getValue().getBytes(UTF_8);
                    value = new String(bytes, UTF_8);
                    eventString += "\nМесто проведения: " + value;

                    property = (Property) outlookEvent.getProperty("DESCRIPTION");
                    bytes = property.getValue().getBytes(UTF_8);
                    value = new String(bytes, UTF_8);
                    eventString += "\nОписание: " + value;
                    message.setText(eventString);
                }
                else
                {
                    Event googleEvent = GoogleUtil.getGoogleEvent(CURRENT_GOOGLE_CALENDAR, call_data.substring(4));

                    // Извлечение и форматирование информации из контейнера Event об одном событии в строку для отправки пользователю.
                    DateTime start = googleEvent.getStart().getDateTime();
                    String eventDescription = googleEvent.getDescription();
                    String eventLocation = googleEvent.getLocation();
                    String eventSummary = googleEvent.getSummary();

                    if (start == null) start = googleEvent.getStart().getDate();
                    if (eventDescription == null) eventDescription = "";
                    if (eventLocation == null) eventLocation = "";
                    if (eventSummary == null) eventSummary = "";

                    eventString += "\n\n"
                            + eventSummary // Заголовок события
                            + "\nВ " + start.toString().substring(11, 16) // Время
                            //+ " " + start.toString().substring(0, 10) // Дата
                            + "\nОписание: " + eventDescription // Описание
                            + "\nМесто проведения: " + eventLocation; // Место проведения события
                    message.setText(eventString);
                }
            }
            else if (call_data.contains("selectCalendarDate"))
            {
                Calendar service;
                Events events;
                String calendDateMinStr = call_data.substring(18) + "T00:00:00.000+03:00";
                DateTime calendDateMin = new DateTime(calendDateMinStr);
                String calendDateMaxStr = call_data.substring(18) + "T23:59:00.000+03:00";
                DateTime calendDateMax = new DateTime(calendDateMaxStr);
                ArrayList<ShortEvent> gevents = new ArrayList<>();
                // Запросить список событий из календаря
                if(!CURRENT_GOOGLE_CALENDAR.equals(""))
                {
                    service = GoogleUtil.getGoogleCalendar();
                    events = GoogleUtil.getGoogleEvents(service, CURRENT_GOOGLE_CALENDAR, calendDateMin, calendDateMax, 15);
                    gevents = GoogleUtil.insertEventsIntoList(events);
                }
                ArrayList<ShortEvent> oevents = new ArrayList<>();
                if (!GOTelegramBot.CURRENT_OUTLOOK_CALENDAR.equals(""))
                {
                    OutlookUtil.downloadOutlookCal(CURRENT_OUTLOOK_CALENDAR);
                    oevents = OutlookUtil.getOutlookEvents("outlookCal.ics", calendDateMin, calendDateMax, 15);
                }

                message.setChatId(chat_id);

                LocalDate showButtonDate = new LocalDate(call_data.substring(18));
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.setTime(showButtonDate.toDate());
                String month = calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, new Locale("ru"));


                gevents.addAll(oevents);
                gevents.sort(Comparator.comparing(ShortEvent::getEventStart));

                if (gevents.isEmpty()) message.setText("Нет событий на " +  showButtonDate.getDayOfMonth() + " " + month);
                else
                {
                    EventKeyboardFactory.generateEventsKeyboard(message, gevents);
                    message.setText("События на " + showButtonDate.getDayOfMonth() + " " + month);
                }
            }
            else if (call_data.equals("monthBack"))
            {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                CURRENT_CALENDAR_DATE = CURRENT_CALENDAR_DATE.minusMonths(1);
                List<List<InlineKeyboardButton>> rowsInline = new EventKeyboardFactory().generateKeyboard(CURRENT_CALENDAR_DATE);

                markupInline.setKeyboard(rowsInline);
                EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
                editMessageReplyMarkup.setChatId(chat_id);
                editMessageReplyMarkup.setInlineMessageId(inl_message_id);
                editMessageReplyMarkup.setMessageId(Math.toIntExact(message_id));
                editMessageReplyMarkup.setReplyMarkup(markupInline);

                try
                {
                    execute(editMessageReplyMarkup);
                }
                catch (TelegramApiException e) {throw new RuntimeException(e);}
            }
            else if (call_data.equals("monthForward"))
            {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                CURRENT_CALENDAR_DATE = CURRENT_CALENDAR_DATE.plusMonths(1);
                List<List<InlineKeyboardButton>> rowsInline = new EventKeyboardFactory().generateKeyboard(CURRENT_CALENDAR_DATE);

                markupInline.setKeyboard(rowsInline);
                EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
                editMessageReplyMarkup.setChatId(chat_id);
                editMessageReplyMarkup.setInlineMessageId(inl_message_id);
                editMessageReplyMarkup.setMessageId(Math.toIntExact(message_id));
                editMessageReplyMarkup.setReplyMarkup(markupInline);

                try
                {
                    execute(editMessageReplyMarkup);
                }
                catch (TelegramApiException e) {throw new RuntimeException(e);}
            }
            else if (call_data.equals("today_button"))
            {
                // Обработка сообщения от пользователя или после нажатия кнопки с соотв. текстом
                Calendar service;

                DateTime now = new DateTime(System.currentTimeMillis());
                Events events;
                String maxTimeStr = now.toString().substring(0, 10) + "T23:59:00.000+03:00";
                DateTime maxTime = new DateTime(maxTimeStr);

                ArrayList<ShortEvent> gevents = new ArrayList<>();

                if(!CURRENT_GOOGLE_CALENDAR.equals(""))
                {
                    service = GoogleUtil.getGoogleCalendar();
                    events = GoogleUtil.getGoogleEvents(service, CURRENT_GOOGLE_CALENDAR, now, maxTime, 15);
                    gevents = GoogleUtil.insertEventsIntoList(events);
                }

                ArrayList<ShortEvent> oevents = new ArrayList<>();
                if(!CURRENT_OUTLOOK_CALENDAR.equals(""))
                {
                    OutlookUtil.downloadOutlookCal(CURRENT_OUTLOOK_CALENDAR);
                    oevents = OutlookUtil.getOutlookEvents("outlookCal.ics", now, maxTime, 15);
                }

                message.setChatId(chat_id);
                gevents.addAll(oevents);
                gevents.sort(Comparator.comparing(ShortEvent::getEventStart));

                if (gevents.isEmpty()) message.setText("Нет событий на сегодня");
                else EventKeyboardFactory.generateEventsKeyboard(message, gevents);
            }
            else if (call_data.equals("tomorrow_button"))
            {
                // Создание новой авторизированной клиентской службы API.
                Calendar service;
                Events events;

                Instant tomorrowDateMinInst = Instant.now().plus(1, ChronoUnit.DAYS);
                String tomorrowDateMinStr = tomorrowDateMinInst.toString().substring(0, 10) + "T00:00:00.000+03:00";
                DateTime tomorrowDateMin = new DateTime(tomorrowDateMinStr);

                Instant tomorrowDateMaxInst = Instant.now().plus(2, ChronoUnit.DAYS);
                String tomorrowDateMaxStr = tomorrowDateMaxInst.toString().substring(0, 10) + "T23:59:00.000+03:00";
                DateTime tomorrowDateMax = new DateTime(tomorrowDateMaxStr);
                ArrayList<ShortEvent> gevents = new ArrayList<>();

                if (!CURRENT_GOOGLE_CALENDAR.equals(""))
                {
                    service = GoogleUtil.getGoogleCalendar();
                    events = GoogleUtil.getGoogleEvents(service, CURRENT_GOOGLE_CALENDAR, tomorrowDateMin, tomorrowDateMax, 15);
                    gevents = GoogleUtil.insertEventsIntoList(events);
                }

                ArrayList<ShortEvent> oevents = new ArrayList<>();
                if (!CURRENT_OUTLOOK_CALENDAR.equals(""))
                {
                    OutlookUtil.downloadOutlookCal(CURRENT_OUTLOOK_CALENDAR);
                    oevents = OutlookUtil.getOutlookEvents("outlookCal.ics", tomorrowDateMin, tomorrowDateMax, 15);
                }

                message.setChatId(chat_id);
                gevents.addAll(oevents);
                gevents.sort(Comparator.comparing(ShortEvent::getEventStart));

                if (gevents.isEmpty()) message.setText("Нет событий на завтра");
                else EventKeyboardFactory.generateEventsKeyboard(message, gevents);
            }

            try
            {
                execute(message); // Отправка пользователю результирующей строки сообщения после обработки
            }
            catch (TelegramApiException e) {e.printStackTrace();}
        }
    }

    // Функция ведения лога в консоли
    private void log(String first_name, String last_name, String user_id, String txt, String bot_answer, String user_username)
    {
        System.out.println("\n_________________________________________________");
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        System.out.println(dateFormat.format(date));
        System.out.println("Message from " + user_username + ", Name: " + first_name + " Surname:" + last_name + " (id = " + user_id + ") \n Text - " + txt);
        System.out.println("Bot answer: \n Text - " + bot_answer);
    }

    private static void writeToFile (String filename, List x) throws IOException
    {
        BufferedWriter outputWriter;
        outputWriter = new BufferedWriter(new FileWriter(filename));

        for (Object item : x)
        {
            outputWriter.write(String.valueOf(item));
            outputWriter.newLine();
        }

        outputWriter.flush();
        outputWriter.close();
    }
}