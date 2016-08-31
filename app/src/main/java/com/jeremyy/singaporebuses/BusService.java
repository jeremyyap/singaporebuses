package com.jeremyy.singaporebuses;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by jeremyy on 8/31/2016.
 */
public class BusService implements Parcelable {
    public String serviceNumber;
    public boolean operating;
    public List<Bus> nextBuses;

    private static final String LOG_TAG = BusService.class.getSimpleName();
    private static final String[] nextBusParams = { "NextBus", "SubsequentBus", "SubsequentBus3" };
    private static final String EMPTY_TEXT = "Seats Available";
    private static final String CROWDED_TEXT = "Standing Available";
    private static final String FULL_TEXT = "Limited Standing";
    private static final SimpleDateFormat parserDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ");

    public BusService() {
        nextBuses = new ArrayList<>();
    }

    private BusService(Parcel in) {
        serviceNumber = in.readString();
        operating = in.readByte() == 1;
        in.readList(nextBuses, Bus.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(serviceNumber);
        dest.writeByte((byte) (operating ? 1: 0));
        dest.writeList(nextBuses);
    }

    public static final Parcelable.Creator<BusService> CREATOR = new Parcelable.Creator<BusService>() {

        @Override
        public BusService createFromParcel(Parcel source) {
            return new BusService(source);
        }

        @Override
        public BusService[] newArray(int size) {
            return new BusService[size];
        }
    };

    public static ArrayList<BusService> fromJson(JSONArray jsonArray) throws JSONException {
        ArrayList<BusService> results = new ArrayList<>();
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject jsonBusService = jsonArray.getJSONObject(i);
            BusService busService = new BusService();
            busService.serviceNumber = jsonBusService.getString("ServiceNo");
            busService.operating = jsonBusService.getString("Status").equals("In Operation");

            for (String nextBusParam : nextBusParams) {
                JSONObject jsonBus = jsonBusService.getJSONObject(nextBusParam);
                Bus bus = new Bus();

                String eta = jsonBus.getString("EstimatedArrival");

                bus.etaMinutes = -1;

                try {
                    long arrivalMillis = parserDateFormat.parse(eta).getTime();
                    long currentMillis = Calendar.getInstance().getTime().getTime();
                    bus.etaMinutes = (int) ((arrivalMillis - currentMillis) / 60000);
                    if (bus.etaMinutes <= 0) bus.etaMinutes = 0;
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                String busLoadDesc = jsonBus.getString("Load");
                switch (busLoadDesc) {
                    case EMPTY_TEXT:
                        bus.load = Bus.Load.EMPTY;
                        break;
                    case CROWDED_TEXT:
                        bus.load = Bus.Load.CROWDED;
                        break;
                    case FULL_TEXT:
                        bus.load = Bus.Load.FULL;
                        break;
                    default:
                        bus.load = Bus.Load.UNKNOWN;
                }

                bus.wheelchairAccessible = jsonBus.getString("Feature").equals("WAB");
                busService.nextBuses.add(bus);
            }

            results.add(busService);
        }
        return results;
    }
}