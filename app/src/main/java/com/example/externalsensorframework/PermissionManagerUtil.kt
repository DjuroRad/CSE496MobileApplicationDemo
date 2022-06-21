package com.example.externalsensorframework
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

/*
 * This class simplifies permission management for the user
 * */
open class AppCompatActivitySensorFrameworkUtil: AppCompatActivity(){
    private val TAG = "PermissionManagerUtil"
    var permissionsGranted = false

    private var permissionStateAction: PermissionStateAction? = null
    //not to be called withing fragment
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        Log.d(TAG, "onRequestPermissionsResult: IN HERE ALOLDLSJF")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this.checkGranted(grantResults)
    }


    /*
     * Function manages permissions for the user.
     * It extends AppCompatActivity and is bound activity it is called in.
     * Just call requestPermissions( ... ) within your activity and missing permissions will be automatically requested
     * @param neededPermissions: MutableSet<String> - put permissions your application needs in here
     * @sample requestPermissions( mutableSet<String>(Manifest.permissions.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT)
     * */
    fun requestPermissions(neededPermissions: MutableSet<String>, permissionStateAction: PermissionStateAction){
        //action changed on each request, if null is passed -> changed to null
        this.permissionStateAction = permissionStateAction
        val neededPermissionsIterator = neededPermissions.iterator()

        Log.d(TAG, "requestPermissions: PERMISSIONS ${neededPermissions}")
        while( neededPermissionsIterator.hasNext() ){
            val prm = neededPermissionsIterator.next()

            //remove permissions that have already been enabled in order to request only the ones that are currently not enabled
            if( this.hasPermissionConsiderAndroidVersion( prm ) )
                neededPermissionsIterator.remove()
            else
                Log.d(TAG, "requestPermissions:DOESN't have permissions $prm")

        }

        Log.d(TAG, "requestPermissions: PERMISSIONS AFTER CHECK ${neededPermissions}")

        if( neededPermissions.isNotEmpty() ){
            for( request in neededPermissions ){
                ActivityCompat.shouldShowRequestPermissionRationale( this, request)
            }
            Log.d(TAG, "requestPermissions: IN HERE ${neededPermissions.toString()}")
            ActivityCompat.requestPermissions( this, neededPermissions.toTypedArray(), 0 )
        }else{
            permissionStateAction?.onGranted()
        }

    }
    private fun checkGranted( grantResults: IntArray ){
        Log.d(TAG, "checkGranted: I AM IN ON CHECK GRATNED")
        //perform action only if all permissions are granted properly
        var allGranted = true
        for( i in grantResults.indices ){
            if( grantResults[i] != PackageManager.PERMISSION_GRANTED ){
                allGranted = false
                break
            }
        }
        if( permissionStateAction == null )
            Log.w(TAG, "checkGranted: DEFINE ACTION TO BE PERFORMED ON PERMISSIONS GRANTED AND ON PERMISSIONS NOT GRANTED", )


        if (allGranted) permissionStateAction?.onGranted()
        else permissionStateAction?.onNotGranted()
    }

    interface PermissionStateAction{
        fun onGranted()
        fun onNotGranted()
    }

}

fun Context.hasPermissionConsiderAndroidVersion(permission:String):Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) //required only for newer versions of android
        ActivityCompat.checkSelfPermission( this, permission ) == PackageManager.PERMISSION_GRANTED
    else
        true

fun Context.hasPermission(permission: String):Boolean =
    ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.lacksPermission(permission: String):Boolean =
    !this.hasPermission(permission)

inline fun <T1: Any, T2: Any, R: Any> TwoVaribaleLet(p1: T1?, p2: T2?, block: (T1, T2)->R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}