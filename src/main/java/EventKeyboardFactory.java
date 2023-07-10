import com.google.api.client.util.DateTime;
import org.joda.time.LocalDate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class EventKeyboardFactory
{
    public static final String       IGNORE                = "ignore!@#$%^&";
    public static final String[]     WD                    = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
    public static final String[]     MONTH_NAME            = {"Январь", "Февраль", "Март",
                                                               "Апрель", "Май", "Июнь",
                                                               "Июль", "Август", "Сентябрь",
                                                               "Октябрь", "Ноябрь", "Декабрь"};

    public List<List<InlineKeyboardButton>> generateKeyboard(LocalDate date)
    {
        if (date == null) return null;

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // row - Month and Year
        List<InlineKeyboardButton> headerRow = new ArrayList<>();

        headerRow.add(InlineKeyboardButton.builder().text(MONTH_NAME[date.getMonthOfYear()-1] + " " + date.getYear()).callbackData(IGNORE).build());
        keyboard.add(headerRow);
        // row - Days of the week
        List<InlineKeyboardButton> daysOfWeekRow = new ArrayList<>();

        for (String day : WD)
            daysOfWeekRow.add(InlineKeyboardButton.builder().text(day).callbackData(IGNORE).build());

        keyboard.add(daysOfWeekRow);

        LocalDate firstDay = date.dayOfMonth().withMinimumValue();

        int shift = firstDay.dayOfWeek().get() - 1;
        int daysInMonth = firstDay.dayOfMonth().getMaximumValue();
        int rows = ((daysInMonth + shift) % 7 > 0 ? 1 : 0) + (daysInMonth + shift) / 7;

        for (int i = 0; i < rows; i++)
        {
            keyboard.add(buildRow(firstDay, shift));
            firstDay = firstDay.plusDays(7 - shift);
            shift = 0;
        }

        List<InlineKeyboardButton> controlsRow = new ArrayList<>();
        controlsRow.add(InlineKeyboardButton.builder().text("<").callbackData("monthBack").build());
        controlsRow.add(InlineKeyboardButton.builder().text(">").callbackData("monthForward").build());
        keyboard.add(controlsRow);

        List<InlineKeyboardButton> soonMeetingRow = new ArrayList<>();
        soonMeetingRow.add(InlineKeyboardButton.builder().text("Сегодня").callbackData("today_button").build());
        soonMeetingRow.add(InlineKeyboardButton.builder().text("Завтра").callbackData("tomorrow_button").build());
        keyboard.add(soonMeetingRow);

        return keyboard;
    }

    private List<InlineKeyboardButton> buildRow(LocalDate date, int shift)
    {
        List<InlineKeyboardButton> row = new ArrayList<>();
        int day = date.getDayOfMonth();
        LocalDate callbackDate = date;

        for (int j = 0; j < shift; j++)
            row.add(InlineKeyboardButton.builder().text(" ").callbackData(IGNORE).build());

        for (int j = shift; j < 7; j++)
        {
            if (day <= (date.dayOfMonth().getMaximumValue()))
            {
                row.add(InlineKeyboardButton.builder().text(Integer.toString(day++)).callbackData("selectCalendarDate" + callbackDate).build());
                callbackDate = callbackDate.plusDays(1);
            }
            else
                row.add(InlineKeyboardButton.builder().text(" ").callbackData(IGNORE).build());
        }
        return row;
    }

    public static void generateEventsKeyboard(SendMessage message, ArrayList<ShortEvent> events)
    {
        /*
            Создание внутричатовой клавиатуры (Inline Keyboard)
            Кнопки - краткая информация о каждом событии на данную дату
        */

        // Извлечение из контейнера Events (информация о календаре) подмассив Items - подробная информация о каждом событии календаря в полученном листе
        DateTime start;
        message.setText("Выберите событие из списка");
        String eventString;
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (ShortEvent event : events)
        {
            start = new DateTime(event.getEventStart());
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            //eventString = item.getSummary() + "\nв " + start.toString().substring(11, 16)/* + " " + start.toString().substring(0, 10)*/;
            eventString = event.getEventSummary() + "\nв " + start.toString().substring(11, 16);

            int idStringEndIndex;
            if (event.getEventId().length() > 60)
                idStringEndIndex = 59;
            else
                idStringEndIndex = event.getEventId().length();
            /*
                Конструктор кнопок внутричатовой клавиатуры. Механизм работы отличается от обычной клавиатуры.
                Так как при нажатии на внутричатовую клавиатуру никакого текста боту не отправляется, проверка содержимого текста невозможна.
                Регистрация нажатий происходит по callbackData
                Содержимое callbackData в данном случае: префикс "meeting_button" + ID события в календаре;
            */
            rowInline.add(InlineKeyboardButton.builder().text(eventString).callbackData("eBtn" + event.getEventId().substring(0, idStringEndIndex)).build());
            // Каждая кнопка занимает один ряд
            rowsInline.add(rowInline);
            markupInline.setKeyboard(rowsInline);
        }
        message.setReplyMarkup(markupInline);
    }
}