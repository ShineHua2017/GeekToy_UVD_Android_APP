package com.shine.geektoy_uvd;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private TextView info;
	//private TextView info2;
	private final int VendorID = 5824;
	private final int ProductID = 1503;
	private UsbManager myUsbManager;
	private UsbDevice myUsbDevice;
	private UsbInterface myInterface;
	private UsbDeviceConnection myDeviceConnection;
	private String status = "";

	int[] imageIds = new int[] { R.drawable.unknown, R.drawable.low,
			R.drawable.mod, R.drawable.high, R.drawable.vh, R.drawable.ext };
	int currentImageId = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		info = (TextView) findViewById(R.id.data);
	    //info2 = (TextView) findViewById(R.id.data2);
		myUsbManager = (UsbManager) getSystemService(USB_SERVICE);
		final ImageView show = (ImageView) findViewById(R.id.show);
		if (enumerateDevice() != false) {
			if (findInterface() != false) {
				openDevice();
				if (status.contains("low")) {
					show.setImageResource(imageIds[1]);
				} else if (status.contains("moderate")) {
					show.setImageResource(imageIds[2]);
				} else if ((status.contains("high"))&&(!status.contains("very"))) {
					show.setImageResource(imageIds[3]);
				} else if (status.contains("veryhigh")) {
					show.setImageResource(imageIds[4]);
				} else if (status.contains("extreme")) {
					show.setImageResource(imageIds[5]);
				} else {
					show.setImageResource(imageIds[0]);
				}
			} else {
				info.setText("UVD未连接");
			}
		} else {
			info.setText("UVD未连接");
		}

	}

	private boolean enumerateDevice() {
		if (myUsbManager != null) {
			HashMap<String, UsbDevice> deviceList = myUsbManager
					.getDeviceList();
			if (!deviceList.isEmpty()) {
				StringBuffer sb = new StringBuffer();
				for (UsbDevice device : deviceList.values()) {
					if (device.getVendorId() == VendorID
							&& device.getProductId() == ProductID) {
						myUsbDevice = device;
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean findInterface() {
		if (myUsbDevice != null) {
			// showTmsg("interfaceCounts : " + myUsbDevice.getInterfaceCount());
			for (int i = 0; i < myUsbDevice.getInterfaceCount(); i++) {
				UsbInterface intf = myUsbDevice.getInterface(i);
				if (intf.getInterfaceClass() == 3
						&& intf.getInterfaceSubclass() == 0
						&& intf.getInterfaceProtocol() == 0) {
					myInterface = intf;
					return true;
					// showTmsg("取得端点信息:" +
					// myInterface.getEndpoint(0).toString());
				}
			}

		}
		return false;
	}

	private void openDevice() {
		if (myInterface != null) {
			UsbDeviceConnection conn = null;

			if (myUsbManager.hasPermission(myUsbDevice)) {
				conn = myUsbManager.openDevice(myUsbDevice);
			}

			if (conn == null) {

			}

			if (conn.claimInterface(myInterface, true)) {
				ByteBuffer getbuf = ByteBuffer.allocate(80);
				CharBuffer getchar = CharBuffer.allocate(80);
				myDeviceConnection = conn;
				// showTmsg("打开设备成功");
				byte[] buffer = new byte[1];
				byte[] getvalue = new byte[1];
				boolean jsvalue = false;
				myDeviceConnection.controlTransfer(0x20, 0x09, 0x0000, 0x0067,
						buffer, buffer.length, 1000);
				myDeviceConnection.controlTransfer(0x20, 0x09, 0x0000, 0x000A,
						buffer, buffer.length, 1000);
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for (int i = 0; i < 80; i++) {
					myDeviceConnection.controlTransfer(0xA0, 0x01, 0x0000,
							0x0000, getvalue, getvalue.length, 1000);
					if (getvalue[0] == 123) {
						jsvalue = true;
					}
					if (jsvalue == true) {
						getbuf.put(getvalue[0]);
					}
					if (jsvalue == true && getvalue[0] == 125) {
						jsvalue = false;
						getbuf.flip();
						break;
					}
				}
				conn.close();
				conn.releaseInterface(myInterface);
				Charset cs = Charset.forName("UTF-8");
				getchar = cs.decode(getbuf);
			    //info2.setText(getchar.toString());
				try {
					JSONObject myJsonObject = new JSONObject(getchar.toString());
					info.setText("实时数据：" + myJsonObject.getString("real_data"));
					status = myJsonObject.getString("exposure_level");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				conn.close();

			}
		}

	}

	/*
	 * private void showTmsg(String msg) { Toast.makeText(MainActivity.this,
	 * msg, Toast.LENGTH_SHORT).show(); }
	 * 
	 * private void showTmsg_int(int msg) { Toast.makeText(MainActivity.this,
	 * msg, Toast.LENGTH_SHORT).show(); }
	 */
	
}
