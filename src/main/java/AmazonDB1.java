import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;


@SuppressWarnings("serial")
public class AmazonDB1 extends HttpServlet {
	private Context androidContext;
	private PrintWriter out;
	
	private String Result;
	private Handler mHandler;
	public 	Handler iHandler;
	private Thread hthread;
    private static final int OUTPUT = 0xFFF;
	
	private Thread threadAmazon1;
	private String Amazon1_ProductTitle;
	private String Amazon1_ProductPrice;
	private IntentBroadcastReceiver IBReceiver_Amazon1;
	private static final int WA_Amazon1_SUCCEED = 0x101;
	private static final int WA_Amazon1_FAILED = 0x10F;
	
	private Thread threadAmazon2;
	private String Amazon2_ProductTitle;
	private String Amazon2_ProductPrice;
	//public 	Handler iHandler;
	private IntentBroadcastReceiver IBReceiver_Amazon2;
	private static final int WA_Amazon2_SUCCEED = 0x201;
	private static final int WA_Amazon2_FAILED = 0x20F;
	
	private Thread threadBookShoppingAdd1;
	private Parcel replyBookShoppingAdd1;
	private String BookShoppingAdd1_Status;	
	private static final int MS_BookShoppingAdd1_SUCCEED = 0x301;
	private static final int MS_BookShoppingAdd1_FAILED = 0x30F;
	
	private Thread threadBookShoppingAdd2;
	private Parcel replyBookShoppingAdd2;
	private String BookShoppingAdd2_Status;	
	private static final int MS_BookShoppingAdd2_SUCCEED = 0x401;
	private static final int MS_BookShoppingAdd2_FAILED = 0x40F;
	
	private static final int SYNCHRONIZE_1 = 0xFF1;
    private boolean Synchronize_1_Amazon2 = false;
    private boolean Synchronize_1_BookAdd1 = false;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		androidContext = (android.content.Context) config.getServletContext().getAttribute("org.mortbay.ijetty.context");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		System.out.println("START_TIME:"+getTimeStamp());
		ServerThreadMonitor stm = ServerThreadMonitor.getInstance();
		if(!stm.isFree) waitserver();
		
		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		out = response.getWriter();
		
		hthread = new Thread() {
			public void run() {
				Looper.prepare();
				mHandler = new Handler() {
					@Override 
					public void handleMessage(Message msg){
						ThreadMonitor tm = ThreadMonitor.getInstance();
						JsonOutputBuilder job = new JsonOutputBuilder();
						Bundle tempBundle;
						String[] tempArray;
						switch (msg.what) {
						case WA_Amazon1_SUCCEED:
							stopThread(threadAmazon1);
							Amazon1_ProductPrice = filterProductPrice(Amazon1_ProductPrice);
							
							prepareAmazon2();
							prepareBookShoppingAdd1();
							System.out.println("T1_TIME:"+getTimeStamp());
							threadAmazon2.run();
							threadBookShoppingAdd1.run();

							break;
						case WA_Amazon1_FAILED:
							stopThread(threadAmazon1);
							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}

							break;
						case WA_Amazon2_SUCCEED:
							stopThread(threadAmazon2);
							Amazon2_ProductPrice = filterProductPrice(Amazon2_ProductPrice);
							
							Synchronize_1_Amazon2 = true;
							
							if(Synchronize_1_Amazon2&&Synchronize_1_BookAdd1)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("Amazon2 finished, other=not yet");
							break;
							
							//prepareBookShoppingAdd2();
							
							//threadBookShoppingAdd2.run();
							
							//prepareAmazonJP();
							
							//threadAmazonJP.run();
							//break;
						case WA_Amazon2_FAILED:
							stopThread(threadAmazon2);
							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}

							break;	
						case MS_BookShoppingAdd1_SUCCEED:
							stopThread(threadBookShoppingAdd1);
							
							tempBundle = replyBookShoppingAdd1.readBundle();
							BookShoppingAdd1_Status = tempBundle.getString("STATUS");
							
							Synchronize_1_BookAdd1 = true;
							
							if(Synchronize_1_Amazon2&&Synchronize_1_BookAdd1)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("AmazonJP finished, other=not yet");
							break;
							
							//mHandler.sendEmptyMessage(OUTPUT);
		                
						case MS_BookShoppingAdd1_FAILED:
							stopThread(threadBookShoppingAdd1);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						
						case SYNCHRONIZE_1:
							Synchronize_1_Amazon2 = false;
							Synchronize_1_BookAdd1 = false;
			        		
							//mHandler.sendEmptyMessage(OUTPUT);
		                	prepareBookShoppingAdd2();
		                	System.out.println("T2_TIME:"+getTimeStamp());
		                	threadBookShoppingAdd2.run();
		                   	break;
						case MS_BookShoppingAdd2_SUCCEED:
							stopThread(threadBookShoppingAdd2);
							
							tempBundle = replyBookShoppingAdd2.readBundle();
							BookShoppingAdd2_Status = tempBundle.getString("STATUS");
							System.out.println("T3_TIME:"+getTimeStamp());
							mHandler.sendEmptyMessage(OUTPUT);
		                	
							break;
						case MS_BookShoppingAdd2_FAILED:
							stopThread(threadBookShoppingAdd2);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						/*case WA_AmazonJP_SUCCEED:
							stopThread(threadAmazonJP);
							AmazonJP_ProductPrice = filterProductPrice(AmazonJP_ProductPrice);
							
							tempBundle = new Bundle();
							tempBundle.putString("ProductPrice", Amazon1_ProductPrice);
							ExchangeRateUSJPWSCParser = new WSComponentParser();
							ExchangeRateUSJPWSCParser.setBundle(tempBundle);
							ExchangeRateUSJPWSCParser.setXML(getExchangeRateUSJPXML());
							
							prepareExchangeRateUSJP();
							System.out.println("T2_TIME:"+getTimeStamp());
							threadExchangeRateUSJP.run();
							
							break;
						case WA_AmazonJP_FAILED:
							stopThread(threadAmazonJP);
							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();
							
							stopThread(hthread);
							synchronized (tm){tm.notify();}

							break;
							
						case WS_ExchangeRateUSJP_SUCCEED:
							stopThread(threadExchangeRateUSJP);
							
		                	tempBundle = replyExchangeRateUSJP.readBundle();
		                	tempArray = tempBundle.getStringArray("YenPrice");
		                	ExchangeRateUSJP_YenPrice = tempArray[0];
		                	
		                	tempBundle = new Bundle();
							tempBundle.putString("ProductPrice", Amazon2_ProductPrice);
							ExchangeRateCAJPWSCParser = new WSComponentParser();
							ExchangeRateCAJPWSCParser.setBundle(tempBundle);
							ExchangeRateCAJPWSCParser.setXML(getExchangeRateCAJPXML());
		                	
		                	prepareExchangeRateCAJP();
		                	System.out.println("T3_TIME:"+getTimeStamp());
							threadExchangeRateCAJP.run();
		                	
		                	//prepareBookShoppingAdd();
		                	//threadBookShoppingAdd.run();
		                	//mHandler.sendEmptyMessage(OUTPUT);
		                	break;
						case WS_ExchangeRateUSJP_FAILED:
							stopThread(threadExchangeRateUSJP);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case WS_ExchangeRateCAJP_SUCCEED:
							stopThread(threadExchangeRateCAJP);
							
		                	tempBundle = replyExchangeRateCAJP.readBundle();
		                	tempArray = tempBundle.getStringArray("YenPrice");
		                	ExchangeRateCAJP_YenPrice = tempArray[0];
		                	System.out.println("T4_TIME:"+getTimeStamp());
		                	//prepareBookShoppingAdd();
		                	//threadBookShoppingAdd.run();
		                	mHandler.sendEmptyMessage(OUTPUT);
		                	break;
						case WS_ExchangeRateCAJP_FAILED:
							stopThread(threadExchangeRateUSJP);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
							*/
						case OUTPUT:
							tempBundle = new Bundle();
							tempBundle.putString("TITLE_1", Amazon1_ProductTitle);
							tempBundle.putString("PRICE_1", Amazon1_ProductPrice);
							tempBundle.putString("STATUS_1", BookShoppingAdd1_Status);
							
							tempBundle.putString("TITLE_2", Amazon2_ProductTitle);
							tempBundle.putString("PRICE_2", Amazon2_ProductPrice);
							tempBundle.putString("STATUS_2", BookShoppingAdd2_Status);
							//tempBundle.putString("PRICE_CAJP", ExchangeRateCAJP_YenPrice);
							
							//tempBundle.putString("TITLE_JP", AmazonJP_ProductTitle);
							//tempBundle.putString("PRICE_JP", AmazonJP_ProductPrice);
							
							
							job.setBundle(tempBundle);
		                	job.setXML(getOutputXML());
		                	Result = job.getJSON();
							
		                	stopThread(hthread);
							synchronized (tm){debug("tm.notified();");tm.notify();}
							
							break;
						default:
							super.handleMessage(msg);
						}
					}
				};
				iHandler = new Handler();
				prepareAmazon1();
				threadAmazon1.start();
				Looper.loop();
			}
		};
		
		stm.isFree = false;
		hthread.start();
		waitfinal();
		out.print(Result);
		
		synchronized (stm){stm.isFree=true;debug("stm.notified();"); stm.notify();}
		System.out.println("FINISH_TIME:"+getTimeStamp());
	}
	
	public void prepareAmazon1(){
		threadAmazon1 = new Thread(new Runnable() {
			public void run() {
				String URL = "http://www.amazon.com/s/ref=nb_sb_noss?force-full-site=1&url=search-alias%3Daps&field-keywords=9781558606432";
				String[] scripts = new String[1];
				

				scripts[0] = 
					"var ProductTitle = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=38&&i<55&&tagArray1[i].className=='title'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('a');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='title'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductTitle.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductTitle,'ProductTitle');"+

				    "var ProductPrice = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('span');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=23&&i<31&&tagArray1[i].className=='newPrice'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('span');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='price'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductPrice.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductPrice,'ProductPrice');"+
				    "window.prach.setfinishstate('true');";

				IBReceiver_Amazon1 = new IntentBroadcastReceiver();
				Intent intent = new Intent("com.prach.mashup.SMA");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String[] msg = {"com.prach.mashup.WAExtractor",
						"RESULTS:OUTPUTS", //0
						"RESULTS:NAMES", //1
						"EXTRA:MODE","EXTRACTION",
						"EXTRA:URL",URL,
						"EXTRAS:SCRIPTS",Function.genExtrasString(scripts)};
				IBReceiver_Amazon1.ResultArrayNameVector.add("OUTPUTS"); //0
				IBReceiver_Amazon1.ResultArrayNameVector.add("NAMES"); //1
				intent.putExtra("MSG", msg);

				IntentFilter IFfinished = new IntentFilter("com.prach.mashup.FINISHED");
				androidContext.registerReceiver(IBReceiver_Amazon1,IFfinished,null,null);
				IBReceiver_Amazon1.ProcessNumber = 0x001;
				IBReceiver_Amazon1.handler = iHandler;
				IBReceiver_Amazon1.finish = iFinished_AmazonPrice1;

				debug("getTitle()->call intent:com.prach.mashup.SMA");
				for (int i = 0; i < msg.length; i++)
					debug("getTitle()->msg["+i+"]:"+msg[i]);

				androidContext.startActivity(intent);
			}
		});
	}
	
	public Runnable iFinished_AmazonPrice1 = new Runnable() {
		public void run(){
			if(IBReceiver_Amazon1.ProcessNumber == 0x001){
				stopThread(threadAmazon1);
				androidContext.unregisterReceiver(IBReceiver_Amazon1);

				int count_resultname = IBReceiver_Amazon1.ResultNameVector.size();
				int count_resultarrayname = IBReceiver_Amazon1.ResultArrayNameVector.size();
				int allcount = count_resultarrayname + count_resultname;

				debug("iFinished.run(0x001)->count_resultname:"+count_resultname);
				debug("iFinished.run(0x001)->count_resultarrayname:"+count_resultarrayname);
				debug("iFinished.run(0x001)->allcount:"+allcount);

				String[][] resultstrings = null;

				if(IBReceiver_Amazon1.ResultStringVector.get(0).equals("RESULT_OK")){
					debug("result OK");
					resultstrings = new String[allcount][];

					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver_Amazon1.ResultStringVector.get(i+1);
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver_Amazon1.ResultStringArrayVector.get(i-count_resultname);
					}

					Amazon1_ProductPrice = Function.getStringByName(
							resultstrings[IBReceiver_Amazon1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon1.getArrayIndexfromName("NAMES")], 
					"ProductPrice");
					Amazon1_ProductTitle = Function.getStringByName(
							resultstrings[IBReceiver_Amazon1.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon1.getArrayIndexfromName("NAMES")], 
					"ProductTitle");
					mHandler.sendEmptyMessage(WA_Amazon1_SUCCEED);
				}else if(IBReceiver_Amazon1.ResultStringVector.get(0).equals("RESULT_CANCELED")){
					debug("result CXL");
					resultstrings = new String[allcount][];
					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
					}
					mHandler.sendEmptyMessage(WA_Amazon1_FAILED);

				}else 
					mHandler.sendEmptyMessage(WA_Amazon1_FAILED);
			}
		}
	};
	
	public void prepareAmazon2(){
		threadAmazon2 = new Thread(new Runnable() {
			public void run() {
				String URL = "http://www.amazon.com/s/ref=nb_sb_noss?force-full-site=1&url=search-alias%3Daps&field-keywords=9781558609235";
				String[] scripts = new String[1];
				
				    

				scripts[0] = 			    
				    "var ProductTitle = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('DIV');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=38&&i<58&&tagArray1[i].className=='title'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('A');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='title'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductTitle.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductTitle,'ProductTitle');"+
				    
				    "var ProductPrice = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('DIV');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=41&&i<59&&tagArray1[i].className=='newPrice'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "/*case 5: single parent&child, single child-tag*/"+
				    "var tagArray2 = parentElement.getElementsByTagName('SPAN');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='price'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductPrice.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductPrice,'ProductPrice');"+
				    "window.prach.setfinishstate('true');";

				IBReceiver_Amazon2 = new IntentBroadcastReceiver();
				Intent intent = new Intent("com.prach.mashup.SMA");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String[] msg = {"com.prach.mashup.WAExtractor",
						"RESULTS:OUTPUTS", //0
						"RESULTS:NAMES", //1
						"EXTRA:MODE","EXTRACTION",
						"EXTRA:URL",URL,
						"EXTRAS:SCRIPTS",Function.genExtrasString(scripts)};
				IBReceiver_Amazon2.ResultArrayNameVector.add("OUTPUTS"); //0
				IBReceiver_Amazon2.ResultArrayNameVector.add("NAMES"); //1
				intent.putExtra("MSG", msg);

				IntentFilter IFfinished = new IntentFilter("com.prach.mashup.FINISHED");
				androidContext.registerReceiver(IBReceiver_Amazon2,IFfinished,null,null);
				IBReceiver_Amazon2.ProcessNumber = 0x001;
				IBReceiver_Amazon2.handler = iHandler;
				IBReceiver_Amazon2.finish = iFinished_AmazonPrice2;

				debug("getTitle()->call intent:com.prach.mashup.SMA");
				for (int i = 0; i < msg.length; i++)
					debug("getTitle()->msg["+i+"]:"+msg[i]);

				androidContext.startActivity(intent);
			}
		});
	}
	
	public Runnable iFinished_AmazonPrice2 = new Runnable() {
		public void run(){
			if(IBReceiver_Amazon2.ProcessNumber == 0x001){
				stopThread(threadAmazon2);
				androidContext.unregisterReceiver(IBReceiver_Amazon2);

				int count_resultname = IBReceiver_Amazon2.ResultNameVector.size();
				int count_resultarrayname = IBReceiver_Amazon2.ResultArrayNameVector.size();
				int allcount = count_resultarrayname + count_resultname;

				debug("iFinished.run(0x001)->count_resultname:"+count_resultname);
				debug("iFinished.run(0x001)->count_resultarrayname:"+count_resultarrayname);
				debug("iFinished.run(0x001)->allcount:"+allcount);

				String[][] resultstrings = null;

				if(IBReceiver_Amazon2.ResultStringVector.get(0).equals("RESULT_OK")){
					debug("result OK");
					resultstrings = new String[allcount][];

					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver_Amazon2.ResultStringVector.get(i+1);
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver_Amazon2.ResultStringArrayVector.get(i-count_resultname);
					}

					Amazon2_ProductPrice = Function.getStringByName(
							resultstrings[IBReceiver_Amazon2.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon2.getArrayIndexfromName("NAMES")], 
					"ProductPrice");
					Amazon2_ProductTitle = Function.getStringByName(
							resultstrings[IBReceiver_Amazon2.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon2.getArrayIndexfromName("NAMES")], 
					"ProductTitle");
					mHandler.sendEmptyMessage(WA_Amazon2_SUCCEED);
				}else if(IBReceiver_Amazon2.ResultStringVector.get(0).equals("RESULT_CANCELED")){
					debug("result CXL");
					resultstrings = new String[allcount][];
					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = new String[1];
						resultstrings[i][0] = "null";
					}
					mHandler.sendEmptyMessage(WA_Amazon2_FAILED);

				}else 
					mHandler.sendEmptyMessage(WA_Amazon2_FAILED);
			}
		}
	};
	
	public void prepareBookShoppingAdd1(){
		threadBookShoppingAdd1 = new Thread(new Runnable() {
			public void run() {
				replyBookShoppingAdd1 = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.BookDatabaseService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x66686601;
					public void onServiceConnected(ComponentName name,IBinder service) {
						debug("BookShoppingAdd Service connected: "+ name.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						String command = "ADD";
						String title = Amazon1_ProductTitle;
						String isbn = "";
						String price = Amazon1_ProductPrice;

						bundle.putString("COMMAND",command);
						bundle.putString("TITLE",title);
						bundle.putString("ISBN",isbn);
						bundle.putString("PRICE",price);

						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyBookShoppingAdd1, 0);
						} catch (RemoteException ex) {
							debug("BookShoppingSummary Service Remote exception when calling service:"+ex.toString());
							res = false;
						}

						if (res)
							mHandler.sendEmptyMessage(MS_BookShoppingAdd1_SUCCEED);
						else
							mHandler.sendEmptyMessage(MS_BookShoppingAdd1_FAILED);
					}
					public void onServiceDisconnected(ComponentName name) {
						debug("BookShoppingSummary Service disconnected: "+ name.flattenToShortString());		
					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("BookShoppingSummary Service could not be connected ");
					mHandler.sendEmptyMessage(MS_BookShoppingAdd1_FAILED);
				}
			}
		});

	}
	
	public void prepareBookShoppingAdd2(){
		threadBookShoppingAdd2 = new Thread(new Runnable() {
			public void run() {
				replyBookShoppingAdd2 = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.BookDatabaseService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x66686601;
					public void onServiceConnected(ComponentName name,IBinder service) {
						debug("BookShoppingAdd Service connected: "+ name.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						String command = "ADD";
						String title = Amazon2_ProductTitle;
						String isbn = "";
						String price = Amazon2_ProductPrice;

						bundle.putString("COMMAND",command);
						bundle.putString("TITLE",title);
						bundle.putString("ISBN",isbn);
						bundle.putString("PRICE",price);

						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyBookShoppingAdd2, 0);
						} catch (RemoteException ex) {
							debug("BookShoppingSummary Service Remote exception when calling service:"+ex.toString());
							res = false;
						}

						if (res)
							mHandler.sendEmptyMessage(MS_BookShoppingAdd2_SUCCEED);
						else
							mHandler.sendEmptyMessage(MS_BookShoppingAdd2_FAILED);
					}
					public void onServiceDisconnected(ComponentName name) {
						debug("BookShoppingSummary Service disconnected: "+ name.flattenToShortString());		
					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("BookShoppingSummary Service could not be connected ");
					mHandler.sendEmptyMessage(MS_BookShoppingAdd2_FAILED);
				}
			}
		});

	}
	
	public String filterProductPrice(String price){
		return price.replaceAll("[^0-9.]", "");
	}
	
	public synchronized void stopThread(Thread t) {
		if (t != null) {
			Thread moribund = t;
			t = null;
			moribund.interrupt();
		}
	}
	
	public void debug(String msg){
		Log.d("AmazonPrice",msg);
	}
	
	public void waitfinal(){
		ThreadMonitor tm = ThreadMonitor.getInstance();
		synchronized (tm) {
			try {
				tm.wait();
			} catch (InterruptedException e) {
				debug("waitfinal()->error="+e.toString());
			}
		}
	}
	
	public void waitserver(){
		ServerThreadMonitor stm = ServerThreadMonitor.getInstance();
		synchronized (stm) {
			try {
				stm.wait();
			} catch (InterruptedException e) {
				debug("waitserver()->error="+e.toString());
			}
		}
	}
	
	/*public String getOutputXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<object>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>BookDatabase.output.STATUS</value>\n"+
		"</object>";
	}*/
	
	public String getOutputXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<object>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>succeed</value>\n"+
		"	<name>Title1</name>\n"+
		"	<value>AmazonPrice1.output.TITLE_1</value>\n"+
		"	<name>Price1</name>\n"+
		"	<value>AmazonPrice1.output.PRICE_1</value>\n"+
		"	<name>Title2</name>\n"+
		"	<value>AmazonPrice2.output.TITLE_2</value>\n"+
		"	<name>Price2</name>\n"+
		"	<value>AmazonPrice2.output.PRICE_2</value>\n"+
		"	<name>Status1</name>\n"+
		"	<value>BookShoppingAdd1.output.STATUS_1</value>\n"+
		"	<name>Status2</name>\n"+
		"	<value>BookShoppingAdd2.output.STATUS_2</value>\n"+
		"</object>";
	}

	public String getErrorXML(){
		return 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
		"<error>\n"+
		"	<name>ResultStatus</name>\n"+
		"	<value>failed</value>\n"+
		"</error>";
	}
	
	
	
	
	
	public String getTimeStamp(){
		Calendar c = Calendar.getInstance();
		
        int hours = c.get(Calendar.HOUR);
        int minutes = c.get(Calendar.MINUTE);
        int seconds = c.get(Calendar.SECOND);
        int mseconds = c.get(Calendar.MILLISECOND);
        return  DateFormat.getDateInstance().format(new Date())+" "+hours + ":"+minutes + ":"+ seconds+":"+mseconds;
	}
}
