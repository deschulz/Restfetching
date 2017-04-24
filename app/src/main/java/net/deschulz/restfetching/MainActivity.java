package net.deschulz.restfetching;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener {

    public static final String TAG = "DesLog";
    private ProgressBar mProgress;
    private Spinner mSpinner;
    private ListView lv;
    private TextView langChoice;
    Lexicon dict;
    Gson gson = new Gson();
    String languages[] = new String[] { "(Choose Language)","Arabic", "French"};
    String myServer = "http://deschulz.net/cgi-bin/ajaxTestProg.py?lang=";


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgress = (ProgressBar) findViewById(R.id.progressBar);


        /* probably the best way to do this is to use a button which displays
         * a spinner when you click it ....
         */
        Spinner mSpinner = (Spinner)findViewById(R.id.spinner);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,languages);

        mSpinner.setAdapter(adapter);
        mSpinner.setSelection(0,false); // this keeps the spinner from firing until actually selected
        mSpinner.setOnItemSelectedListener(this);


    }

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        Log.i(TAG,"onItemSelected() " + position);
        // position 0 is the label
        if (position > 0) {
            new RetrieveURLTask().execute(myServer
                    + ((String) parent.getItemAtPosition(position)).toLowerCase());
        }

    }

    public void onNothingSelected(AdapterView<?> parent) {
        // this should not happen ??
        Log.i(TAG,"onNothingSelected()");
    }

    /* this is just to demonstrate how we can serialize to/from a POJO using Gson */
    class Lexicon {
        String language;
        Map<String,String> dictionary;

        Lexicon() {
            dictionary = new HashMap<String,String>();
        }

        String getLanguage() {
            return language;
        }
        Map<String,String> getDictionary() {
            return dictionary;
        }
    }


    /*
     * First arg is the class passed to doInBackground via execute(), Second arg is passwd
     * to onProgressUpdate, and third arg is the return type of doInBackground.   The return
     * of doInBackground is passed to onPostExecute.
     */
    class RetrieveURLTask extends AsyncTask<String, Integer, String > {

        @Override
        protected void onPreExecute() {

            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.i(TAG,"onProgressUpdate()");
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                Log.i(TAG,"doInBackground()");
                int count = urls.length;
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {

                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + " ");
                    }

                    Log.i(TAG,sb.toString());

                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        return null;
                    }

                    return sb.toString();

                } finally {
                    urlConnection.disconnect();
                }


            } catch (Exception e) {

                return null;
            }
        }

        @Override
        protected void onPostExecute(String jso) {

            mProgress.setVisibility(View.INVISIBLE);
            Log.i(TAG,"onPostExecute()");

            /* fiddle is actually called by the UI thread */
            fiddle(jso);
        }
    }

    /*
     *  This seems to work perfectly!
     */
    public void fiddle(String jso) {

        Lexicon dict = gson.fromJson(jso,Lexicon.class);

        Log.i(TAG,"POJO Success: language = " + dict.getLanguage() );
        for (String s : dict.dictionary.keySet()) {
            Log.i(TAG,"key = " + s + " value = " + dict.dictionary.get(s));
        }

        String jsonString = gson.toJson(jso);
        Log.i(TAG,"jsonString = " + jsonString);

        DictionaryAdapter adapter = new DictionaryAdapter(dict.getDictionary());
        ListView lv = (ListView) findViewById(R.id.dictview);
        lv.setAdapter(adapter);
    }

    /*******************   Build a custom ArrayAdapter for a Map ********************/
    /*  this is similar but a little different from starting with an array ... */
    public class DictionaryAdapter extends BaseAdapter {
        private final ArrayList mData;

        // Create an array of the keys.  Later in the getView we can extract the keys
        public DictionaryAdapter(Map<String,String> dict) {
            mData = new ArrayList();
            mData.addAll(dict.entrySet());
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Map.Entry<String,String> getItem(int position) {
            return (Map.Entry) mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            // not sure what this is for?
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View result;
            if (convertView == null) {
                result = LayoutInflater.from(parent.getContext()).inflate(R.layout.dictentry,
                        parent, false);
            }
            else {
                result = convertView;
            }

            Map.Entry<String,String> item = getItem(position);
            TextView lookup = (TextView) result.findViewById(R.id.lookup);
            TextView trans = (TextView) result.findViewById(R.id.translation);
            lookup.setText(item.getKey());
            trans.setText(item.getValue());
            return result;
        }
    }

}
