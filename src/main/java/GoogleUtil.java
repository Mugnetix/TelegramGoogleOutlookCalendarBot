import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.joda.time.Instant;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleUtil
{
    private static final String       APPLICATION_NAME      = "Google Calendar API Java";
    private static final JsonFactory  JSON_FACTORY          = GsonFactory.getDefaultInstance();
    private static final String       TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES                = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);

    //Credentials используются для получения токена доступа с серверов авторизации Google, чтобы приложение могло вызывать Google Workspace API.
    public static        String       CREDENTIALS_FILE_PATH = "";

    public static boolean isValidGoogleId (String calendarID)
    {
        Calendar service;
        service = getGoogleCalendar();
        boolean validId = false;
        try
        {
            Events events = service.events().list(calendarID)
                    .setMaxResults(1)
                    .setTimeMin(new DateTime(Instant.now().getMillis()))
                    .execute();
            if (!events.getSummary().isEmpty())
                validId = true;
        }
        catch (IOException ignored) {}

        return validId;
    }

    public static Events getGoogleEvents (Calendar calendarService, String calendarID, DateTime timeMin, DateTime timeMax, int eventsAmount)
    {
        // Запросить список событий из календаря
        Events events = new Events();
        try
        {
            events = calendarService.events().list(calendarID) //
                    .setMaxResults(eventsAmount)
                    .setTimeMin(timeMin) // Точка отсчёта по дате-времени
                    .setTimeMax(timeMax) // Конец отсчёта по дате-времени
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
        }
        catch (IOException e) {
            System.out.println(e);
            //throw new RuntimeException(e);
        }

        return events;
    }

    public static Event getGoogleEvent (String googleCalendarID, String eventID)
    {
        Calendar service;
        service = getGoogleCalendar();
        Event event;

        try
        {
            // Запрос на получение от календаря события с заданным ID
            event = service.events().get(googleCalendarID, eventID).execute();
        }
        catch (IOException e) {throw new RuntimeException(e);}

        return event;
    }

    private static Credential getGoogleCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException
    {
        // Загрузка файла учётных данных на сервер Google.
        //InputStream in = Main.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        InputStream in = null;
        try
        {
            in = new FileInputStream(CREDENTIALS_FILE_PATH);
        }
        catch (FileNotFoundException e) {
            System.out.println("\nERROR:" + CREDENTIALS_FILE_PATH + " not found!" +
                    "\nFile does not exist in the root directory or file name in bot_config.txt is wrong");
            System.exit(1);
        }
        //if (in == null) System.out.println("Resource not found: " + CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        //System.out.println("credentials loaded");

        // Построение и отправка запроса на получение токена авторизации.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static Calendar getGoogleCalendar()
    {
        final NetHttpTransport HTTP_TRANSPORT;
        Calendar calend;

        try
        {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        }
        catch (GeneralSecurityException | IOException e) {throw new RuntimeException(e);}

        try
        {
            calend = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        catch (IOException e) {throw new RuntimeException(e);}

        return calend;
    }

    public static ArrayList<ShortEvent> insertEventsIntoList(Events googleEvents)
    {
        ArrayList<ShortEvent> eventList = new ArrayList<>();
        List<Event> items = googleEvents.getItems();
        DateTime start;

        for (Event item : items)
        {
            start = item.getStart().getDateTime();
            if (start == null) start = item.getStart().getDate();
            ShortEvent shortEvent = new ShortEvent(item.getId(), item.getSummary(), start.getValue());
            eventList.add(shortEvent);
        }

        return eventList;
    }
}
