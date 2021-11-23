import static com.google.firebase.firestore.Query.Direction.DESCENDING;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class Summary {
    private String TAG = "Summary";
    private String collectionName = "summaries";
    private Set<String> mIds; //TODO: initialize ids

    private int totalCount; //TODO: initialize totalCount

    private String mId;
    private Date mDate;
    private Map<String, Integer> mCounters;

    Query collection = FirebaseFirestore.getInstance().collection(collectionName).orderBy("date", DESCENDING);

    public Summary(){
        mDate = new Date();
        mIds = new HashSet<>();
        mCounters = new HashMap<>();
        //create()
        mCounters.put("SITUPS", 0);
        mCounters.put("PUSHUPS", 0);
        mCounters.put("JUMPING_JACKS", 0);
        mCounters.put("SQUATS", 0);

    }

    public Summary(String id, Date date, Map<String, Integer> counters) {
        if (!mIds.contains(id)) {
            mIds.add(id);
        }
        mId = id;
        mDate = date;
        mCounters = counters;
    }

//    public void fromDocument(DocumentSnapshot document) {
//        mId = document.getId();
//        mDate = document.getDate("date");
//        mCounters = document.get("counters"));
//    }

//    factory Summary.create() {
//        return Summary(null, new Date(), {
//        SITUPS: 0,
//        PUSHUPS: 0,
//        JUMPING_JACKS: 0,
//        SQUATS: 0,
//        });
//    }

    Future<Summary> add() async {
        DocumentReference docRef = await Firestore.instance
            .collection(collectionName)
            .add({'date': this.date, 'counters': this.counters});
        this.id = docRef.documentID;
        return this;
    }

//    Future<Summary> pull() async {
//        DocumentSnapshot docRef = await Firestore.instance
//            .collection(collectionName)
//            .document(this.id)
//            .get();
//        return Summary.fromDocument(docRef);
//    }

    Future<Summary> submit() async {
        if (this.id == null) {
        this.add();
        }
        await Firestore.instance
            .collection(collectionName)
            .document(this.id)
            .setData({
        'date': this.date,
        'counters': this.counters
        }, merge: true);
        return this;
    }

    Summary reset() {
        Log.d(TAG, "resetting summary for date: " + new SimpleDateFormat("HH:mm:ss", Locale.US).format(date));
        for (var key in this.counters.keys) {
        this.counters[key] = 0;
        }
        return this;
    }

    Summary increment(String label) {
        this.counters[label] += 1;
        return this;
    }

    Summary decrement(String label) {
        this.counters[label] -= 1;
        return this;
    }

    bool get isFromToday {
        final today = DateTime.now();
        return (this.date.year == today.year &&
            this.date.month == today.month &&
            this.date.day == today.day);
    }

    String toAccessibleString() {
        String result = '';
        var entries = this.counters.entries.where((entry) => entry.value > 0);
        for (var entry in entries) {
        if (result != '') {
            if (entry.key == entries.last.key)
            result += ' and ';
            else
            result += ', ';
        }
        result += '${entry.value} ${entry.key}';
        }
        if (result == '')
        result = 'no exercise';
        return result;
    }
}
