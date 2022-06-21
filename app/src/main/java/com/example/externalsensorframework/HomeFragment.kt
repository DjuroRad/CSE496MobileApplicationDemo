package com.example.externalsensorframework

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.externalsensorframework.sensor_framework.communication_channel.bluetooth.BluetoothChannelManager
import com.example.externalsensorframework.sensor_framework.communication_channel.bluetooth.BluetoothPermissionManager
import com.example.externalsensorframework.sensor_framework.communication_channel.bluetooth.BluetoothPermissionManager.isLocationEnabled
import com.example.externalsensorframework.sensor_framework.communication_channel.bluetooth.BluetoothPermissionManager.nearbyDevicesList
import com.example.externalsensorframework.sensor_framework.communication_channel.bluetooth.BluetoothPermissionManager.registerForPairingIntent
import com.example.externalsensorframework.sensor_framework.communication_channel.bluetooth.BluetoothPermissionManager.registerReceiverExtra
import com.example.externalsensorframework.sensor_framework.communication_channel.bluetooth.BluetoothPermissionManager.unregisterReceiverExtra

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/*
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    private val bluetoothChannelManager: BluetoothChannelManager = BluetoothChannelManager()
    private var nearbyBluetoothDevicesTextView: TextView? = null
    private var nearbyDevicesListView: ListView? = null
    private var nearbyDevicesListTemporary: List<BluetoothDevice> = listOf()

    private var param1: String? = null
    private var param2: String? = null
    private val TAG = "HomeFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        val bluetoothAction = object: BluetoothPermissionManager.SystemServiceAction{
            override fun onEnabled() {
                BluetoothPermissionManager.enableLocation()
            }
            override fun onDisabled() {}
        }

        val locationAction = object: BluetoothPermissionManager.SystemServiceAction{
            override fun onEnabled() {
                registerReceiverExtra(requireActivity() as AppCompatActivity)
                registerForPairingIntent( requireActivity() as AppCompatActivity, broadcastReceiverPairing)
                setTextOfNearbyDevices(requireActivity())//observers nearbyDevice hashmap and updates the UI
            }
            override fun onDisabled() {}
        }

        BluetoothPermissionManager.registerToEnableBluetoothAndLocation(requireActivity() as AppCompatActivity, bluetoothAction, locationAction)

        (requireActivity() as AppCompatActivitySensorFrameworkUtil).requestPermissions( mutableSetOf( Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            object : AppCompatActivitySensorFrameworkUtil.PermissionStateAction {
                override fun onGranted() {
                    if( !BluetoothPermissionManager.isBluetoothEnabled(requireContext()) || isLocationEnabled(requireContext()) == false)
                        BluetoothPermissionManager.enableBluetoothAndLocation()
                    else {
                        registerReceiverExtra(requireActivity() as AppCompatActivity)
                        updateListDevices()
                        setTextOfNearbyDevices(requireActivity())//observers nearbyDevice hashmap and updates the UI
                    }
                }
                override fun onNotGranted() {

                }
            }
        )
    }

    private fun updateListDevices(){

    }

    private fun setTextOfNearbyDevices(context: Context) {
        nearbyDevicesList.observe(this, Observer { newNearbyDevices ->
            updateTextViewOfNearbyDevice(newNearbyDevices);
        })
    }
    @SuppressLint("MissingPermission")
    private fun updateTextViewOfNearbyDevice(newNearbyDevices: MutableList<BluetoothDevice>) {
        var newText = ""
        var list: MutableList<String> = mutableListOf<String>()

        if( newNearbyDevices.size == 0 )
            newText = "Scanning nearby devices, curentlly none found"
        else{
            newText = "Number of nearby devices: " + newNearbyDevices.size + "\n\n"
            for ( pair in newNearbyDevices ) {
                list.add("Name = ${pair.name}\nMAC: ${pair.address}")
            }

            var listAdapter: ListAdapter = ArrayAdapter(requireContext(), R.layout.list_item,list)
            nearbyDevicesListView?.adapter = listAdapter

            nearbyDevicesListView?.setOnItemClickListener { adapterView, view, i, l ->
                Toast.makeText(requireContext(), "Connecting to $i: ${(view as TextView).text.toString().trim().split(' ', '\n')[2]}", Toast.LENGTH_LONG).show()
                if( newNearbyDevices[i].bondState == BluetoothDevice.BOND_BONDED ) {
                    onPaired(newNearbyDevices[i])
                }
                else
                    newNearbyDevices[i].createBond()
            }
        }

        nearbyBluetoothDevicesTextView?.text = newText

    }

    //method is if devices are already paired or
    //when 2 devices are paired
    private fun onPaired(bluetoothDevice: BluetoothDevice){
        val bundle = bundleOf("bluetooth-device" to bluetoothDevice)
        findNavController().navigate(R.id.action_homeFragment2_to_connectedFragment, bundle)
    }

    private val broadcastReceiverPairing = object: BroadcastReceiver(){
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            action?.let{
                if(it == BluetoothDevice.ACTION_BOND_STATE_CHANGED){
                    val bluetoothDevice: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if(bluetoothDevice?.bondState == BluetoothDevice.BOND_BONDED){
                        Toast.makeText(context, "bonded with a bluetooth device", Toast.LENGTH_SHORT).show()
                        onPaired(bluetoothDevice)
                        unregisterReceiverExtra(this, requireActivity())
                        //unregister receiver after this
                    }
                    if(bluetoothDevice?.bondState == BluetoothDevice.BOND_BONDING){
                        Toast.makeText(context, "Bonding in process", Toast.LENGTH_SHORT).show()
                    }
                    if(bluetoothDevice?.bondState == BluetoothDevice.BOND_NONE){
                        Toast.makeText(context, "bonding failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        nearbyBluetoothDevicesTextView = view?.findViewById(R.id.bluetoothDeviceList)
        nearbyDevicesListView = view?.findViewById(R.id.nearbyDevicesListView)
        return view
    }

    companion object {
        /*
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}