package info.guardianproject.phoneypot.sensors;

/**
 * Created by n8fr8 on 3/10/17.
 */

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import info.guardianproject.phoneypot.PreferenceManager;
import info.guardianproject.phoneypot.model.EventTrigger;
import info.guardianproject.phoneypot.sensors.media.AudioRecorderTask;
import info.guardianproject.phoneypot.sensors.media.MicSamplerTask;
import info.guardianproject.phoneypot.sensors.media.MicrophoneTaskFactory;
import info.guardianproject.phoneypot.service.MonitorService;


public final class MicrophoneMonitor implements MicSamplerTask.MicListener {


    private MicSamplerTask microphone;

    /**
     * Object used to fetch application dependencies
     */
    private PreferenceManager prefs;

    /**
     * Threshold for the decibels sampled
     */
    private double mNoiseThreshold = 70.0;

    /**
     * Messenger used to communicate with alert service
     */
    private Messenger serviceMessenger = null;

    private Context context;

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i("MicrophoneFragment", "SERVICE CONNECTED");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            serviceMessenger = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName arg0) {
            Log.i("MicrophoneFragment", "SERVICE DISCONNECTED");
            serviceMessenger = null;
        }
    };


    public MicrophoneMonitor(Context context)
    {
        this.context = context;

        prefs = new PreferenceManager(context);

        if (prefs.getMicrophoneSensitivity().equals("High")) {
            mNoiseThreshold = 40;
        } else if (prefs.getMicrophoneSensitivity().equals("Medium")) {
            mNoiseThreshold = 60;
        }
        else
        {
            try {
                //maybe it is a threshold value?
                mNoiseThreshold = Double.parseDouble(prefs.getMicrophoneSensitivity());
            }
            catch (Exception e){}
        }

        context.bindService(new Intent(context,
                MonitorService.class), mConnection, Context.BIND_ABOVE_CLIENT);

        try {
            microphone = MicrophoneTaskFactory.makeSampler(context);
            microphone.setMicListener(this);
            microphone.execute();
        } catch (MicrophoneTaskFactory.RecordLimitExceeded e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



    }

    public void stop (Context context)
    {
        context.unbindService(mConnection);
        microphone.cancel(true);
    }


    public void onSignalReceived(short[] signal) {

		/*
		 * We do and average of the 512 samples
		 */
        int total = 0;
        int count = 0;
        for (short peak : signal) {
            //Log.i("MicrophoneFragment", "Sampled values are: "+peak);
            if (peak != 0) {
                total += Math.abs(peak);
                count++;
            }
        }
      //  Log.i("MicrophoneFragment", "Total value: " + total);
        int average = 0;
        if (count > 0) average = total / count;
		/*
		 * We compute a value in decibels
		 */
        double averageDB = 0.0;
        if (average != 0) {
            averageDB = 20 * Math.log10(Math.abs(average) / 1);
        }

        if (averageDB > mNoiseThreshold) {

            if (!MicrophoneTaskFactory.isRecording()) {
                try {
                    AudioRecorderTask audioRecorderTask = MicrophoneTaskFactory.makeRecorder(context);
                    audioRecorderTask.start();

                    Message message = new Message();
                    message.what = EventTrigger.MICROPHONE;
                    message.getData().putString("path",audioRecorderTask.getAudioFilePath());
                    try {
                        if (serviceMessenger != null)
                            serviceMessenger.send(message);
                    } catch (RemoteException e) {
                        // Cannot happen
                    }
                } catch (MicrophoneTaskFactory.RecordLimitExceeded rle) {
                    Log.w("MicrophoneMonitor", "We are already recording!");
                }
            }
        }
    }

    public void onMicError() {
        Log.e("MicrophoneActivity", "Microphone is not ready");
    }
}