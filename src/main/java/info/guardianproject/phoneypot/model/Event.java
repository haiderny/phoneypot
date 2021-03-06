package info.guardianproject.phoneypot.model;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class Event extends SugarRecord {

    Date mStartTime;

    @Ignore
    ArrayList<EventTrigger> mEventTriggers;

    public final static long EVENT_WINDOW_TIME = 1000 * 60; //1 minutes

    public Event ()
    {
        mStartTime = new Date();
        mEventTriggers = new ArrayList<>();
    }

    public Date getStartTime ()
    {
        return mStartTime;
    }

    public void addEventTrigger (EventTrigger eventTrigger)
    {
        mEventTriggers.add(eventTrigger);
        eventTrigger.setEventId(getId());
    }

    public ArrayList<EventTrigger> getEventTriggers ()
    {
        if (mEventTriggers.size() == 0) {
            List<EventTrigger> eventTriggers = EventTrigger.find(EventTrigger.class, "M_EVENT_ID = ?", getId() + "");

            for (EventTrigger et : eventTriggers)
                mEventTriggers.add(et);

        }

        return mEventTriggers;
    }
    /**
    * Are we within the time window of this event, or should we start a new event?
     */
    public boolean insideEventWindow (Date now)
    {
        if (mEventTriggers.size() == 0)
            return now.getTime() - mStartTime.getTime() <= EVENT_WINDOW_TIME;
        else
            return now.getTime() - mEventTriggers.get(mEventTriggers.size()-1).getTriggerTime().getTime() <= EVENT_WINDOW_TIME;
    }
}
