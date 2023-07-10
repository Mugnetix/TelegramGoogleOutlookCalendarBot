import com.google.api.client.util.DateTime;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.CalendarComponent;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OutlookUtil
{
    public static ArrayList<ShortEvent> getOutlookEvents(String outlookCalFile, DateTime minTime, DateTime maxTime, int eventsAmount)
    {
        ArrayList<ShortEvent> eventList = new ArrayList<>();

        try
        {
            CalendarBuilder builder = new CalendarBuilder();
            final UnfoldingReader fReader = new UnfoldingReader(new FileReader(outlookCalFile), true);
            net.fortuna.ical4j.model.Calendar calendar = builder.build(fReader);
            fReader.close();
            List<CalendarComponent> componentList = calendar.getComponents("VEVENT");
            int counter = 0;

            for (Component component : componentList)
            {
                ShortEvent shortEvent = new ShortEvent();
                PropertyList eventProps = component.getProperties("UID", "SUMMARY", "DTSTART");

                Property property = (Property) eventProps.getProperty("UID");
                shortEvent.setEventId("@" + property.getValue());

                property = (Property) eventProps.getProperty("SUMMARY");
                byte[] bytes = property.getValue().getBytes(StandardCharsets.UTF_8);
                String value = new String(bytes, StandardCharsets.UTF_8);
                shortEvent.setEventSummary(value);

                property = (Property) eventProps.getProperty("DTSTART");
                String s = property.getValue();
                String eventStartTimeStr = s.substring(0, 4) + "-"
                        + s.substring(4, 6) + "-"
                        + s.substring(6, 11) + ":"
                        + s.substring(11, 13) + ":"
                        + s.substring(13) + ".000+03:00";

                DateTime eventStartTime = new DateTime(eventStartTimeStr);


                if (eventStartTime.getValue() >= minTime.getValue() && eventStartTime.getValue() <= maxTime.getValue())
                {
                    shortEvent.setShortEventStart(eventStartTime.getValue());
                    eventList.add(shortEvent);
                    counter++;
                }
                if (counter == eventsAmount) break;
            }
        }
        catch (Throwable t) {t.printStackTrace();}
        return eventList;
    }

    public static PropertyList getOutlookEvent(String outlookCalFile, String eventID)
    {
        PropertyList eventProps = new PropertyList<>();
        try
        {
            CalendarBuilder builder = new CalendarBuilder();
            final UnfoldingReader fReader = new UnfoldingReader(new FileReader(outlookCalFile), true);
            net.fortuna.ical4j.model.Calendar calendar = builder.build(fReader);
            fReader.close();

            for (final Component component : calendar.getComponents("VEVENT"))
            {
                Property property = component.getProperties("UID").getProperty("UID");

                if (property.getValue().startsWith(eventID))
                {
                    eventProps = component.getProperties("SUMMARY", "DTSTART", "LOCATION", "DESCRIPTION");
                    break;
                }
            }
        }
        catch (Throwable t) {t.printStackTrace();}

        return eventProps;
    }

    public static boolean downloadOutlookCal(String urlString)
    {
        URL icalUrl;
        boolean downloadSuccess = false;

        try
        {
            icalUrl = new URL(urlString);
            InputStream in = icalUrl.openStream();
            ReadableByteChannel rbc = Channels.newChannel(in);
            FileOutputStream fos = new FileOutputStream("outlookCal.ics");
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            downloadSuccess = true;
        }
        catch (IOException e) {return downloadSuccess;}

        return downloadSuccess;
    }
}
