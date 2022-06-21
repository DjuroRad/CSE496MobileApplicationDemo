package com.example.externalsensorframework

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.icu.number.Precision
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.externalsensorframework.sensor_framework.SensorFrameworkManager
import com.example.externalsensorframework.sensor_framework.client.ClientCommunicationThread
import com.example.externalsensorframework.sensor_framework.communication_channel.bluetooth.BluetoothChannelManager
import com.example.externalsensorframework.sensor_framework.sensors.SensorObserver
import com.example.externalsensorframework.sensor_framework.sensors.SensorType
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.nio.ByteBuffer

/*
 * A simple [Fragment] subclass.
 * Use the [ConnectedFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ConnectedFragment : Fragment(), SensorObserver{

    private val TAG = "ConnectedFragment"
    private var bluetoothDevice: BluetoothDevice? = null

    //layout component elements' declaration
    private var initializeConnectionButton: Button? = null
    private var connectButton: Button? = null
    private var startReadButton: Button? = null
    private var stopReadButton: Button? = null


    private var connectSensorButton: Button? = null;
    private var connectSensorIdEditText: EditText? = null;

    private var disconnectSensorButton: Button? = null;
    private var disconnectSensorIdEditText: EditText? = null;

    private var configureButton: Button? = null
    private var configureSensorIdEditText: EditText? = null
    private var configureSampleRateEditText: EditText? = null
    private var configureRadioGroupFormatted: RadioGroup? = null
    private var configureRadioGroupPrecision: RadioGroup? = null
    private var precisionOffset: EditText? = null

    private var isConnectedButton: Button? = null;
    private var isConnectedSensorIdEditText: EditText? = null

    private var disconnectButton: Button? = null
    private var sensorValueTextView: TextView? = null

    private var availableSensorsTextView: TextView? = null

    private val graphs: MutableList<GraphView> = mutableListOf()
    private var lineGraphSeries: MutableList<LineGraphSeries<DataPoint>> = mutableListOf()
    //framework related declarations
    private val bluetoothChannelManager: BluetoothChannelManager = BluetoothChannelManager()
    private var connectionOpened: Boolean = false;

    private var sensorFrameworkManager:SensorFrameworkManager? = null

    //these sensors will be received upon connecting
    private var availableSensors: MutableLiveData< List<ClientCommunicationThread.SensorEntry> > = MutableLiveData()


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = this.arguments
        if (bundle != null)
            bluetoothDevice = bundle.getParcelable<BluetoothDevice>("bluetooth-device")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_connected, container, false)

        //initialize layout components
        connectButton = view?.findViewById(R.id.connectButton)
        startReadButton = view?.findViewById(R.id.startReadButton)
        stopReadButton = view?.findViewById(R.id.stopReadButton)

        disconnectButton = view?.findViewById(R.id.disconnectButton)

        sensorValueTextView = view?.findViewById(R.id.sensorValueTextView)
        initializeConnectionButton = view?.findViewById(R.id.initConnect)

        connectSensorButton = view?.findViewById(R.id.connectSensorButton)
        connectSensorIdEditText = view?.findViewById(R.id.connectSensorIdEditText);

        disconnectSensorButton = view?.findViewById(R.id.disconnectSensorButton)
        disconnectSensorIdEditText = view?.findViewById(R.id.disconnectSensorIdEditText);

        configureButton = view?.findViewById(R.id.configureButton)
        configureSensorIdEditText = view?.findViewById(R.id.sensorEditTextId)
        configureSampleRateEditText = view?.findViewById(R.id.configureSampleRate)
        configureRadioGroupFormatted = view?.findViewById(R.id.radioGroupFormatted)
        configureRadioGroupPrecision = view?.findViewById(R.id.radioGroupPrecision)
        precisionOffset = view?.findViewById(R.id.precisionOffsetEditText)


        isConnectedButton = view?.findViewById(R.id.isConnectedButton)
        isConnectedSensorIdEditText = view?.findViewById(R.id.isConnectedIdEditText)

        availableSensorsTextView = view?.findViewById(R.id.availableSensors)

        graphs.add( view?.findViewById(R.id.graph1) as GraphView )
        graphs.add( view?.findViewById(R.id.graph2) as GraphView )
        graphs.add( view?.findViewById(R.id.graph3) as GraphView )
        graphs.add( view?.findViewById(R.id.graph4) as GraphView )
        graphs.add( view?.findViewById(R.id.graph5) as GraphView )

        //watch for available sensors and show on UI
        availableSensors.observe(viewLifecycleOwner, Observer { sensors ->
            var availableSensorsText: String = "Available sensors:\n"
            for( (index, sensor) in sensors.withIndex() ){
                availableSensorsText += "Type: ${sensor.sensorType}, ID: ${sensor.sensorID}, Sample size: ${sensor.dataSampleByteLength}, minValue:${sensor.minValue}, maxValue:${sensor.maxValue}\n"

                val graphTitle: String = "ID: " + sensor.sensorID.toString() + ", TYPE: ${sensor.sensorType.toString()}"

                lineGraphSeries.add(LineGraphSeries())
                graphs[index].title = graphTitle
            }
            availableSensorsTextView?.text = availableSensorsText
        })


        connectButton?.setOnClickListener {
            sensorFrameworkManager?.connect(2000)
        }

        isConnectedButton?.setOnClickListener {
            val sensorID: Int = isConnectedSensorIdEditText?.text.toString().toInt()
            sensorFrameworkManager?.isConnected(sensorID)
        }

        connectSensorButton?.setOnClickListener {
            val sensorID: Int = connectSensorIdEditText?.text.toString().toInt()
            sensorFrameworkManager?.connectSensor(sensorID)
        }

        disconnectSensorButton?.setOnClickListener {
            val sensorID: Int = disconnectSensorIdEditText?.text.toString().toInt()
            sensorFrameworkManager?.disconnectSensor(sensorID)
        }

        configureButton?.setOnClickListener {
            val sensorID = configureSensorIdEditText?.text.toString().toInt()//get the id
            val sampleRate = configureSampleRateEditText?.text.toString().toInt()//get sample length

            val formatted: Boolean = configureRadioGroupFormatted?.checkedRadioButtonId == R.id.radioYes

            val offset: Double = precisionOffset?.text.toString().toDouble()
            val precision: SensorObserver.SensorPrecision = SensorObserver.SensorPrecision( if (configureRadioGroupPrecision?.checkedRadioButtonId == R.id.radioPrecise) SensorObserver.PRECISION.PRECISE else SensorObserver.PRECISION.IMPRECISE_OPTIMIZED, offset )

            val sensorConfiguration: SensorObserver.SensorConfiguration = SensorObserver.SensorConfiguration(sampleRate, precision, formatted);
            sensorFrameworkManager?.configure(sensorID, sensorConfiguration);

        }
        //setup functionality for buttons using the framework provided methods
        startReadButton?.setOnClickListener {
            sensorFrameworkManager?.startRead()
        }

        stopReadButton?.setOnClickListener {
            sensorFrameworkManager?.stopRead()
        }

        disconnectButton?.setOnClickListener {
            sensorFrameworkManager?.disconnect()
        }
        initializeConnectionButton?.setOnClickListener {
            openConnectionToRemoteDevice()
            sensorFrameworkManager = SensorFrameworkManager( this as SensorObserver, bluetoothChannelManager.inputStreamServer!!, bluetoothChannelManager.outputStreamClient!!);
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        bluetoothChannelManager.closeConnection()
    }

    private fun openConnectionToRemoteDevice(){
        //open connection with the device
        bluetoothDevice?.let { bluetoothChannelManager.openBluetoothConnection(it) }
        //after connecting test the framework
        if( bluetoothChannelManager.connectionEstablished ){
            if( bluetoothChannelManager.isConnected() ){
                connectionOpened = true
            }
        }
    }

    override fun onConnected(availableSensors2: List<ClientCommunicationThread.SensorEntry>) {
        this.availableSensors.postValue(availableSensors2)
    }

    override fun onSensorConnected(sensorID: Int) {
        requireActivity().apply {
            Toast.makeText(this, "Sensor with id $sensorID connected", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSensorDisconnected(sensorID: Int) {
        requireActivity().apply {
            // Stuff that updates the UI
            Toast.makeText(this, "Sensor with id $sensorID disconnected", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSensorDataChanged(sensorData: SensorObserver.SensorData) {
        Log.d(TAG, "onSensorDataChanged: ${sensorData.value.toInt()}")
        var value: Number = sensorData.value.toInt()
        if( sensorData.sensorType == SensorType.SENSOR_CUSTOM_ANALOG )
            value = ByteBuffer.wrap(sensorData.rawData).double
        // Stuff that updates the UI
        val newText = "Value: " + value.toString() + "\nFormatted: ${sensorData.formattedData}" + "\nSensor type: " + sensorData.sensorType + "\nID: " + sensorData.sensorID;

        requireActivity().runOnUiThread{
            sensorValueTextView?.text = newText
            updateGraph(sensorData)
        }
    }

    private fun updateGraph(sensorData: SensorObserver.SensorData){

        availableSensors.value?.let {
            for( (index, sensor) in it.withIndex()){
                if( sensor.sensorID == sensorData.sensorID ){

                    val lastX = lineGraphSeries[index].highestValueX
                    var newDataPoint:DataPoint? = null

                    if( sensorData.sensorType == SensorType.SENSOR_CUSTOM_ANALOG )
                        newDataPoint = DataPoint((lastX+1).toDouble(), ByteBuffer.wrap(sensorData.rawData).double)
                    else
                        newDataPoint = DataPoint((lastX+1).toDouble(), sensorData.value.toDouble())

                    lineGraphSeries[index].appendData(newDataPoint, true, 40);
                    graphs[index].addSeries(lineGraphSeries[index])
                }
            }
        }
    }

    override fun isConnected(sensorId: Int, connected: Boolean) {
        val text: String = "sensor with sensor id: $sensorId is " + (if (connected) "connected" else "not connected")

        requireActivity().runOnUiThread(Runnable{
            Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
        })
    }

    override fun onError(sensorError: SensorObserver.SensorError) {
        //implement on error also so that we can see all the data here
    }

    override fun onConfigured(sensorID: Int, configured: Boolean) {
        requireActivity().runOnUiThread(Runnable{
            val text: String = "Sensor with id of $sensorID" + if(configured) " successfully configured" else " not configurable"
            Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
        })
    }
}