package xbee.udootest;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import me.palazzetti.adktoolkit.AdkManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import wlsvm.WLSVM;


public class MainActivity extends Activity{

//	private static final String TAG = "UDOO_AndroidADKFULL";	 

    private AdkManager mAdkManager;

    private ToggleButton buttonLED;
    private TextView distance;
    private TextView pulse;
    private TextView position;
    private int correct = 0;
    private int incorrect = 0;
    private boolean isTraining = false;
    private boolean trained = false;
    private AdkReadTask mAdkReadTask;
    private WLSVM svm = new WLSVM();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAdkManager = new AdkManager((UsbManager) getSystemService(Context.USB_SERVICE));

//		register a BroadcastReceiver to catch UsbManager.ACTION_USB_ACCESSORY_DETACHED action
        registerReceiver(mAdkManager.getUsbReceiver(), mAdkManager.getDetachedFilter());

        buttonLED = (ToggleButton) findViewById(R.id.toggleButtonLED);
        distance  = (TextView) findViewById(R.id.textView_distance);
        pulse  = (TextView) findViewById(R.id.textView_pulse);
        position  = (TextView) findViewById(R.id.textView_position);

        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File f = new File(root,"iris_train.arff");
        BufferedReader inputReader;
        try {
            inputReader = new BufferedReader(new FileReader(f));
            Instances data = new Instances(inputReader);
            data.setClassIndex(data.numAttributes() -1);
            Classifier ibk = new IBk();
            ibk.buildClassifier(data);
            f = new File(root, "iris_test.arff");
            inputReader = new BufferedReader(new FileReader(f));
            Instances test = new Instances(inputReader);
            test.setClassIndex(test.numAttributes()-1);
            int[][] scores = new int[][]{new int[]{0,0,0}, new int[]{0,0,0}, new int[]{0,0,0}};
            for (int i=0; i < test.numInstances(); i++){
                double pred = ibk.classifyInstance(test.instance(i));
                double act = test.instance(i).classValue();
                scores[(int) act][(int) pred]++;

            }
            ((TextView) findViewById(R.id.e00)).setText("" + scores[0][0]);
            ((TextView) findViewById(R.id.e01)).setText("" + scores[0][1]);
            ((TextView) findViewById(R.id.e02)).setText("" + scores[0][2]);
            ((TextView) findViewById(R.id.e10)).setText("" + scores[1][0]);
            ((TextView) findViewById(R.id.e11)).setText("" + scores[1][1]);
            ((TextView) findViewById(R.id.e12)).setText("" + scores[1][2]);
            ((TextView) findViewById(R.id.e20)).setText("" + scores[2][0]);
            ((TextView) findViewById(R.id.e21)).setText("" + scores[2][1]);
            ((TextView) findViewById(R.id.e22)).setText("" + scores[2][2]);


        }catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        mAdkManager.open();

        mAdkReadTask = new AdkReadTask();
        mAdkReadTask.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdkManager.close();

        mAdkReadTask.pause();
        mAdkReadTask = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mAdkManager.getUsbReceiver());
    }

    // ToggleButton method - send message to SAM3X
    public void blinkLED(View v){
        if (buttonLED.isChecked()) {
            // writeSerial() allows you to write a single char or a String object.
            mAdkManager.writeSerial("1");
        } else {
            mAdkManager.writeSerial("0");
        }
    }

    public void onClickTrain(View v) {
        ((Button) v).setText(!isTraining ? "Stop training" : "train");
        isTraining = !isTraining;
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File f = new File(root,"iris_train.arff");
        BufferedReader inputReader;
        try {
            inputReader = new BufferedReader(new FileReader(f));
            Instances data = new Instances(inputReader);
            data.setClassIndex(data.numAttributes() -1);

            svm.buildClassifier(data);
            f = new File(root, "iris_test.arff");
            inputReader = new BufferedReader(new FileReader(f));
            Instances test = new Instances(inputReader);
            test.setClassIndex(test.numAttributes()-1);
            int[][] scores = new int[][]{new int[]{0,0,0}, new int[]{0,0,0}, new int[]{0,0,0}};
            correct = 0;
            int total = 0;
            for (int i=0; i < test.numInstances(); i++){
                double pred = svm.classifyInstance(test.instance(i));
                double act = test.instance(i).classValue();
                scores[(int) act][(int) pred]++;
                total++;
                if (pred == act) {
                    correct++;
                }

            }
            ((TextView) findViewById(R.id.e00)).setText("" + scores[0][0]);
            ((TextView) findViewById(R.id.e01)).setText("" + scores[0][1]);
            ((TextView) findViewById(R.id.e02)).setText("" + scores[0][2]);
            ((TextView) findViewById(R.id.e10)).setText("" + scores[1][0]);
            ((TextView) findViewById(R.id.e11)).setText("" + scores[1][1]);
            ((TextView) findViewById(R.id.e12)).setText("" + scores[1][2]);
            ((TextView) findViewById(R.id.e20)).setText("" + scores[2][0]);
            ((TextView) findViewById(R.id.e21)).setText("" + scores[2][1]);
            ((TextView) findViewById(R.id.e22)).setText("" + scores[2][2]);
            ((TextView) findViewById(R.id.accuracy)).setText("" + correct/(double) total * 100 + "%");

        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onClickClassify(View v){
        Attribute Attribute1 = new Attribute("sepallength");
        Attribute Attribute2 = new Attribute("sepalwidth");
        Attribute Attribute3 = new Attribute("petallength");
        Attribute Attribute4 = new Attribute("petalwidth");

        FastVector fvClassVal = new FastVector(3);
        fvClassVal.addElement("Iris-setosa");
        fvClassVal.addElement("Iris-versicolor");
        fvClassVal.addElement("Iris-virginia");
        Attribute ClassAttribute = new Attribute("class",fvClassVal);

        FastVector fvWekaAttributes = new FastVector(5);
        fvWekaAttributes.addElement(Attribute1);
        fvWekaAttributes.addElement(Attribute2);
        fvWekaAttributes.addElement(Attribute3);
        fvWekaAttributes.addElement(Attribute4);
        fvWekaAttributes.addElement(ClassAttribute);

        Instances testingSet = new Instances("TestingInstance", fvWekaAttributes,1);
        testingSet.setClassIndex(testingSet.numAttributes()-1);

        Instance iExample = new Instance(testingSet.numAttributes());

        iExample.setValue((Attribute)fvWekaAttributes.elementAt(0),Double.parseDouble(((EditText) findViewById(R.id.SLE)).getText().toString()));
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(1),Double.parseDouble(((EditText) findViewById(R.id.SWE)).getText().toString()));
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(2),Double.parseDouble(((EditText) findViewById(R.id.PLE)).getText().toString()));
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(3),Double.parseDouble(((EditText) findViewById(R.id.PWE)).getText().toString()));
        iExample.setValue((Attribute)fvWekaAttributes.elementAt(4),"Iris-setosa");

        testingSet.add(iExample);

        try {
            double pred = svm.classifyInstance(testingSet.instance(0));
            ((TextView) findViewById(R.id.pred)).setText("" + pred);
        }catch(Exception e){
            e.printStackTrace();
        }

    }
    /*
     * We put the readSerial() method in an AsyncTask to run the
     * continuous read task out of the UI main thread
     */
    private class AdkReadTask extends AsyncTask<Void, String, Void> {

        private boolean running = true;

        public void pause(){
            running = false;
        }

        protected Void doInBackground(Void... params) {
//	    	Log.i("ADK demo bi", "start adkreadtask");
            while(running) {
                publishProgress(mAdkManager.readSerial()) ;
            }
            return null;
        }

        protected void onProgressUpdate(String... progress) {

            float pulseRate= (int)progress[0].charAt(0);
            float oxygenLvl= (int)progress[0].charAt(1);
            float pos= (int)progress[0].charAt(2);
            int max = 255;
            if (pulseRate>max) pulseRate=max;
            if (oxygenLvl>max) oxygenLvl=max;
            if (pos>max) pos=max;

//            DecimalFormat df = new DecimalFormat("#.#");
            distance.setText(pulseRate + " (bpm)");
            pulse.setText(oxygenLvl + " (pct)");
            position.setText(pos + "");
        }
    }



}
