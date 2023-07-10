import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main
{
    public static long chat_ID;
    public static List<Long> notification_chat_IDs = new ArrayList<>();

    public static void main(String[] args) throws TelegramApiException, IOException {
        // Создание сессии в Telegram Bot API
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        GOTelegramBot bot = new GOTelegramBot();
        // Чтение данных из файлов и заполнение глобальных переменных программы
        List<String> linesFromFile = readFromFile("notif_id.txt");

        for (String readIdLine : linesFromFile)
        {
            try
            {
                notification_chat_IDs.add(Long.parseLong(readIdLine));
            }
            catch (NumberFormatException ignored){}
        }

        linesFromFile = readFromFile("calendar_config.txt");
        if (!linesFromFile.isEmpty())
        {
            for (String lineId : linesFromFile)
            {
                if (lineId.startsWith("outlook=") && lineId.length() > 8)
                    GOTelegramBot.CURRENT_OUTLOOK_CALENDAR = lineId.substring(8);
                else if (lineId.startsWith("google=") && lineId.length() > 7)
                    GOTelegramBot.CURRENT_GOOGLE_CALENDAR = lineId.substring(7);
            }
        }
        linesFromFile = readFromFile("bot_config.txt");
        for (String readConfLine : linesFromFile)
        {
            if (readConfLine.startsWith("token=") && readConfLine.length() > 6)
                GOTelegramBot.BOT_TOKEN = readConfLine.substring(6);
            else if (readConfLine.startsWith("credentials=") && readConfLine.length() > 12)
                GoogleUtil.CREDENTIALS_FILE_PATH += readConfLine.substring(12);
        }

        // Регистрация бота
        try
        {
            System.out.println("Bot registration in process...");
            botsApi.registerBot(bot);
        }
        catch (TelegramApiException e)
        {
            System.out.println("\nERROR:" + e.getMessage()
                    + "\nCheck bot token in bot_config.txt");
            System.exit(1);
        }
        System.out.println("Bot successfully registered");
        long timeShiftFix = 60000 - System.currentTimeMillis() % 60000;

        // Ежеминутная проверка событий из календарей по времени начала события и отправка уведомлений в чаты
        Runnable notifCheck = new Runnable()
        {
            public void run()
            {
                if (!notification_chat_IDs.isEmpty())
                {
                    Calendar service;
                    DateTime start;
                    long minTimeFix = 1000 - System.currentTimeMillis() % 1000;
                    DateTime notification_minTime = new DateTime(System.currentTimeMillis() + 1799000L + minTimeFix);
                    Events events;
                    DateTime notification_maxTime = new DateTime(notification_minTime.getValue() + 59000L);
                    String eventString = null;
                    ArrayList<ShortEvent> gevents = new ArrayList<>();

                    if (!GOTelegramBot.CURRENT_GOOGLE_CALENDAR.equals(""))
                    {
                        service = GoogleUtil.getGoogleCalendar();
                        events = GoogleUtil.getGoogleEvents(service, GOTelegramBot.CURRENT_GOOGLE_CALENDAR, notification_minTime, notification_maxTime, 5);
                        long geventStart;
                        if (!events.getItems().isEmpty())
                        {
                            gevents = GoogleUtil.insertEventsIntoList(events);
                            geventStart = events.getItems().get(0).getStart().getDateTime().getValue();
                            if (geventStart < notification_minTime.getValue())
                                events.clear();
                        }
                    }

                    ArrayList<ShortEvent> oevents = new ArrayList<>();
                    if (!GOTelegramBot.CURRENT_OUTLOOK_CALENDAR.equals(""))
                    {
                        OutlookUtil.downloadOutlookCal(GOTelegramBot.CURRENT_OUTLOOK_CALENDAR);
                        oevents = OutlookUtil.getOutlookEvents("outlookCal.ics", notification_minTime, notification_maxTime, 5);
                    }
                    gevents.addAll(oevents);
                    long timeDiff;

                    if (!gevents.isEmpty())
                    {
                        Collections.sort(gevents, Comparator.comparing(ShortEvent::getEventStart));
                        for (int i = 0; i < gevents.size(); i++)
                        {
                            timeDiff = gevents.get(i).getEventStart() - System.currentTimeMillis();
                            if (timeDiff <= 1800000L && timeDiff > 1740000L)
                            {
                                if (i == 0 || eventString == null)
                                    eventString = "Событие(-я) через 30 минут!";
                                else
                                    eventString += "\n______________________________";
                                // Извлечение из контейнера Events (информация о календаре) подмассив Items - подробная информация о каждом событии календаря в полученном листе

                                if (gevents.get(i).getEventId().startsWith("@"))
                                {
                                    PropertyList outlookEvent = OutlookUtil.getOutlookEvent("outlookCal.ics", gevents.get(i).getEventId().substring(1));

                                    Property property = (Property) outlookEvent.getProperty("SUMMARY");
                                    eventString += "\n\n" + property.getValue();

                                    property = (Property) outlookEvent.getProperty("DTSTART");
                                    String s = property.getValue();
                                    eventString += "\nВ "
                                            + s.substring(9, 11) + ":"
                                            + s.substring(11, 13);

                                    property = (Property) outlookEvent.getProperty("LOCATION");
                                    eventString += "\nМесто проведения: " + property.getValue();

                                    property = (Property) outlookEvent.getProperty("DESCRIPTION");
                                    eventString += "\nОписание: " + property.getValue();
                                }
                                else
                                {
                                    Event googleEvent = GoogleUtil.getGoogleEvent(GOTelegramBot.CURRENT_GOOGLE_CALENDAR, gevents.get(i).getEventId());

                                    // Извлечение и форматирование информации из контейнера Event об одном событии в строку для отправки пользователю.
                                    start = googleEvent.getStart().getDateTime();
                                    String eventSummary = googleEvent.getSummary();
                                    String eventDescription = googleEvent.getDescription();
                                    String eventLocation = googleEvent.getLocation();

                                    if (eventSummary == null) eventSummary = "";
                                    if (eventDescription == null) eventDescription = "";
                                    if (eventLocation == null) eventLocation = "";
                                    if (start == null) start = googleEvent.getStart().getDate();

                                    eventString += "\n\n"
                                            + eventSummary // Заголовок события
                                            + "\nВ " + start.toString().substring(11, 16) // Время
                                            //+ " " + start.toString().substring(0, 10) // Дата
                                            + "\nМесто проведения: " + eventLocation // Место проведения события
                                            + "\nОписание: " + eventDescription; // Описание
                                }
                            }
                        }
                    }
                    if (eventString != null)
                    {
                        for (Long notification_chat_id : notification_chat_IDs)
                        {
                            SendMessage sendMessage = new SendMessage(notification_chat_id.toString(), eventString);
                            try
                            {
                                bot.execute(sendMessage);
                            }
                            catch (TelegramApiException e) {throw new RuntimeException(e);}
                        }
                    }
                }
            }
        };
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(notifCheck, timeShiftFix, 60000, TimeUnit.MILLISECONDS);
    }


    // Метод построчного чтения файла в массив
    private static List<String> readFromFile (String filename) throws IOException
    {
        BufferedReader fileReader;
        List<String> readList = new ArrayList<>();
        String line;
        try
        {
            fileReader = new BufferedReader(new FileReader(filename));
            line = fileReader.readLine();
            while(line != null)
            {
                readList.add(line);
                line = fileReader.readLine();
            }
            fileReader.close();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("\nERROR:" + filename + " not found!"
                + "\nCreate " + filename + " in the root folder or check the name of existing one");
            System.exit(1);
        }
        return readList;
    }
}
