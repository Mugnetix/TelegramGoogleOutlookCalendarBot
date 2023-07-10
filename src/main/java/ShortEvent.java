public class ShortEvent
{
    private String shortEventId;
    private String shortEventSummary;
    private long shortEventStart;

    public ShortEvent(String shortEventId, String shortEventSummary, long shortEventStart)
    {
        this.shortEventId = shortEventId;
        this.shortEventSummary = shortEventSummary;
        this.shortEventStart = shortEventStart;
    }

    public ShortEvent()
    {
        this.shortEventId = null;
        this.shortEventSummary = null;
        this.shortEventStart = -1;
    }

    public String getEventId()
    {
        return this.shortEventId;
    }
    public String getEventSummary()
    {
        return this.shortEventSummary;
    }
    public long getEventStart()
    {
        return this.shortEventStart;
    }
    public void setEventId(String shortEventId)
    {
        this.shortEventId = shortEventId;
    }
    public void setEventSummary(String shortEventSummary)
    {
        this.shortEventSummary = shortEventSummary;
    }
    public void setShortEventStart(long shortEventStart)
    {
        this.shortEventStart = shortEventStart;
    }
}
