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
public class AmazonDB4 extends HttpServlet {
	private Context androidContext;
	private PrintWriter out;
	
	private String Result;
	private Handler mHandler;
	public 	Handler iHandler;
	private Thread hthread;
    private static final int OUTPUT = 0xFFF;
	
	private Thread threadAmazon;
	private String Amazon_ProductTitle1;
	private String Amazon_ProductPrice1;
	private String Amazon_ProductTitle2;
	private String Amazon_ProductPrice2;
	private String Amazon_ProductTitle3;
	private String Amazon_ProductPrice3;
	private String Amazon_ProductTitle4;
	private String Amazon_ProductPrice4;
	private IntentBroadcastReceiver IBReceiver_Amazon;
	private static final int WA_Amazon_SUCCEED = 0x101;
	private static final int WA_Amazon_FAILED = 0x10F;
	
	private Thread threadBookShoppingAdd1;
	private Parcel replyBookShoppingAdd1;
	private String BookShoppingAdd1_Status;	
	private static final int MS_BookShoppingAdd1_SUCCEED = 0x201;
	private static final int MS_BookShoppingAdd1_FAILED = 0x20F;
	
	private Thread threadBookShoppingAdd2;
	private Parcel replyBookShoppingAdd2;
	private String BookShoppingAdd2_Status;	
	private static final int MS_BookShoppingAdd2_SUCCEED = 0x301;
	private static final int MS_BookShoppingAdd2_FAILED = 0x30F;
	
	private Thread threadBookShoppingAdd3;
	private Parcel replyBookShoppingAdd3;
	private String BookShoppingAdd3_Status;	
	private static final int MS_BookShoppingAdd3_SUCCEED = 0x401;
	private static final int MS_BookShoppingAdd3_FAILED = 0x40F;
	
	private Thread threadBookShoppingAdd4;
	private Parcel replyBookShoppingAdd4;
	private String BookShoppingAdd4_Status;	
	private static final int MS_BookShoppingAdd4_SUCCEED = 0x501;
	private static final int MS_BookShoppingAdd4_FAILED = 0x50F;
	
	private static final int SYNCHRONIZE_1 = 0xFF1;
    private boolean Synchronize_1_BookAdd1 = false;
    private boolean Synchronize_1_BookAdd2 = false;
    private boolean Synchronize_1_BookAdd3 = false;
    private boolean Synchronize_1_BookAdd4 = false;
    
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
						case WA_Amazon_SUCCEED:
							stopThread(threadAmazon);
							Amazon_ProductPrice1 = filterProductPrice(Amazon_ProductPrice1);
							Amazon_ProductPrice2 = filterProductPrice(Amazon_ProductPrice2);
							Amazon_ProductPrice3 = filterProductPrice(Amazon_ProductPrice3);
							Amazon_ProductPrice4 = filterProductPrice(Amazon_ProductPrice4);
							
							//prepareAmazon2();
							prepareBookShoppingAdd1();
							prepareBookShoppingAdd2();
							prepareBookShoppingAdd3();
							prepareBookShoppingAdd4();
							System.out.println("T1_TIME:"+getTimeStamp());
							threadBookShoppingAdd1.run();
							threadBookShoppingAdd2.run();
							threadBookShoppingAdd3.run();
							threadBookShoppingAdd4.run();
							//threadAmazon2.run();
							

							break;
						case WA_Amazon_FAILED:
							stopThread(threadAmazon);
							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}

							break;
						/*case WA_Amazon2_SUCCEED:
							stopThread(threadAmazon2);
							Amazon2_ProductPrice = filterProductPrice(Amazon2_ProductPrice);
							
							prepareBookShoppingAdd1();
							System.out.println("T2_TIME:"+getTimeStamp());
							threadBookShoppingAdd1.run();
							
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

							break;	*/
						case MS_BookShoppingAdd1_SUCCEED:
							stopThread(threadBookShoppingAdd1);
							
							tempBundle = replyBookShoppingAdd1.readBundle();
							BookShoppingAdd1_Status = tempBundle.getString("STATUS");
							
							Synchronize_1_BookAdd1 = true;
							
							if(Synchronize_1_BookAdd1&&Synchronize_1_BookAdd2&&Synchronize_1_BookAdd3&&Synchronize_1_BookAdd4)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("BookAdd1 finished, other=not yet");
							break;
						case MS_BookShoppingAdd1_FAILED:
							stopThread(threadBookShoppingAdd1);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case MS_BookShoppingAdd2_SUCCEED:
							stopThread(threadBookShoppingAdd2);
							
							tempBundle = replyBookShoppingAdd2.readBundle();
							BookShoppingAdd2_Status = tempBundle.getString("STATUS");
							
							Synchronize_1_BookAdd2 = true;
							
							if(Synchronize_1_BookAdd1&&Synchronize_1_BookAdd2&&Synchronize_1_BookAdd3&&Synchronize_1_BookAdd4)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("BookAdd2 finished, other=not yet");
							break;
						case MS_BookShoppingAdd2_FAILED:
							stopThread(threadBookShoppingAdd2);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;	
						case MS_BookShoppingAdd3_SUCCEED:
							stopThread(threadBookShoppingAdd3);
							
							tempBundle = replyBookShoppingAdd3.readBundle();
							BookShoppingAdd3_Status = tempBundle.getString("STATUS");
							
							Synchronize_1_BookAdd3 = true;
							
							if(Synchronize_1_BookAdd1&&Synchronize_1_BookAdd2&&Synchronize_1_BookAdd3&&Synchronize_1_BookAdd4)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("BookAdd3 finished, other=not yet");
							break;
						case MS_BookShoppingAdd3_FAILED:
							stopThread(threadBookShoppingAdd3);

							job.setErrorXML(getErrorXML());
							Result = job.getErrorJSON();

							stopThread(hthread);
							synchronized (tm){tm.notify();}
							break;
						case MS_BookShoppingAdd4_SUCCEED:
							stopThread(threadBookShoppingAdd4);
							
							tempBundle = replyBookShoppingAdd4.readBundle();
							BookShoppingAdd4_Status = tempBundle.getString("STATUS");
							
							Synchronize_1_BookAdd4 = true;
							
							if(Synchronize_1_BookAdd1&&Synchronize_1_BookAdd2&&Synchronize_1_BookAdd3&&Synchronize_1_BookAdd4)
			        			mHandler.sendEmptyMessage(SYNCHRONIZE_1);
							else
								debug("BookAdd4 finished, other=not yet");
							break;
						case MS_BookShoppingAdd4_FAILED:
							stopThread(threadBookShoppingAdd4);

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
						case SYNCHRONIZE_1:
							Synchronize_1_BookAdd1 = false;
							Synchronize_1_BookAdd2 = false;
							Synchronize_1_BookAdd3 = false;
							Synchronize_1_BookAdd4 = false;
							
							//mHandler.sendEmptyMessage(OUTPUT);
		                	
		                	System.out.println("T2_TIME:"+getTimeStamp());
		                	mHandler.sendEmptyMessage(OUTPUT);
		                   	break;
						case OUTPUT:
							tempBundle = new Bundle();
							tempBundle.putString("TITLE_1", Amazon_ProductTitle1);
							tempBundle.putString("PRICE_1", Amazon_ProductPrice1);
							tempBundle.putString("STATUS_1", BookShoppingAdd1_Status);
							tempBundle.putString("TITLE_2", Amazon_ProductTitle2);
							tempBundle.putString("PRICE_2", Amazon_ProductPrice2);
							tempBundle.putString("STATUS_2", BookShoppingAdd2_Status);
							tempBundle.putString("TITLE_3", Amazon_ProductTitle3);
							tempBundle.putString("PRICE_3", Amazon_ProductPrice3);
							tempBundle.putString("STATUS_3", BookShoppingAdd3_Status);
							tempBundle.putString("TITLE_4", Amazon_ProductTitle4);
							tempBundle.putString("PRICE_4", Amazon_ProductPrice4);
							tempBundle.putString("STATUS_4", BookShoppingAdd4_Status);
							
							//tempBundle.putString("TITLE_2", Amazon2_ProductTitle);
							//tempBundle.putString("PRICE_2", Amazon2_ProductPrice);
							//tempBundle.putString("STATUS_2", BookShoppingAdd2_Status);
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
				prepareAmazon();
				threadAmazon.start();
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
	
	public void prepareAmazon(){
		threadAmazon = new Thread(new Runnable() {
			public void run() {
				String URL = "http://www.amazon.com/s/ref=nb_sb_noss?force-full-site=1url=search-alias%3Daps&field-keywords=android+programming&x=0&y=0";
				String[] scripts = new String[1];
				

				scripts[0] = 
					"var ProductTitle1 = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=41&&i<61&&tagArray1[i].className=='title'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "var tagArray2 = parentElement.getElementsByTagName('a');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='title'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductTitle1.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductTitle1,'ProductTitle1');"+

				    "var ProductPrice1 = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=42&&i<52&&tagArray1[i].className=='newPrice'){"+
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
				    "        ProductPrice1.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductPrice1,'ProductPrice1');"+
				    
				    "var ProductTitle2 = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=56&&i<76&&tagArray1[i].className=='title'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "var tagArray2 = parentElement.getElementsByTagName('a');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='title'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductTitle2.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductTitle2,'ProductTitle2');"+

				    "var ProductPrice2 = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=57&&i<77&&tagArray1[i].className=='newPrice'){"+
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
				    "        ProductPrice2.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductPrice2,'ProductPrice2');"+
				    
				    "var ProductTitle3 = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=72&&i<92&&tagArray1[i].className=='title'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "var tagArray2 = parentElement.getElementsByTagName('a');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='title'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductTitle3.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductTitle3,'ProductTitle3');"+

				    "var ProductPrice3 = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=73&&i<93&&tagArray1[i].className=='newPrice'){"+
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
				    "        ProductPrice3.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductPrice3,'ProductPrice3');"+
				    
				    "var ProductTitle4 = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=92&&i<112&&tagArray1[i].className=='title'){"+
				    "        parentElement = tagArray1[i];"+
				    "        break;"+
				    "    }"+
				    "}"+
				    "if(parentElement==undefined)"+
				    "    window.prach.setfinishstate('false');"+
				    "var tagArray2 = parentElement.getElementsByTagName('a');"+
				    "var childElement;"+
				    "for(var i=0;i<tagArray2.length;i++){"+
				    "    if(i==0&&tagArray2[i].className=='title'){"+
				    "        childElement = tagArray2[i];"+
				    "        ProductTitle4.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductTitle4,'ProductTitle4');"+

				    "var ProductPrice4 = new Array();"+
				    "var tagArray1 = document.getElementsByTagName('div');"+
				    "var parentElement;"+
				    "for(var i=0;i<tagArray1.length;i++){"+
				    "    if(i>=93&&i<113&&tagArray1[i].className=='newPrice'){"+
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
				    "        ProductPrice4.push(childElement.textContent);"+
				    "    }"+
				    "}"+
				    "window.prach.addOutputArray(ProductPrice4,'ProductPrice4');"+
				    
				    "window.prach.setfinishstate('true');";

				IBReceiver_Amazon = new IntentBroadcastReceiver();
				Intent intent = new Intent("com.prach.mashup.SMA");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				String[] msg = {"com.prach.mashup.WAExtractor",
						"RESULTS:OUTPUTS", //0
						"RESULTS:NAMES", //1
						"EXTRA:MODE","EXTRACTION",
						"EXTRA:URL",URL,
						"EXTRAS:SCRIPTS",Function.genExtrasString(scripts)};
				IBReceiver_Amazon.ResultArrayNameVector.add("OUTPUTS"); //0
				IBReceiver_Amazon.ResultArrayNameVector.add("NAMES"); //1
				intent.putExtra("MSG", msg);

				IntentFilter IFfinished = new IntentFilter("com.prach.mashup.FINISHED");
				androidContext.registerReceiver(IBReceiver_Amazon,IFfinished,null,null);
				IBReceiver_Amazon.ProcessNumber = 0x001;
				IBReceiver_Amazon.handler = iHandler;
				IBReceiver_Amazon.finish = iFinished_Amazon;

				debug("getTitle()->call intent:com.prach.mashup.SMA");
				for (int i = 0; i < msg.length; i++)
					debug("getTitle()->msg["+i+"]:"+msg[i]);

				androidContext.startActivity(intent);
			}
		});
	}
	
	public Runnable iFinished_Amazon = new Runnable() {
		public void run(){
			if(IBReceiver_Amazon.ProcessNumber == 0x001){
				stopThread(threadAmazon);
				androidContext.unregisterReceiver(IBReceiver_Amazon);

				int count_resultname = IBReceiver_Amazon.ResultNameVector.size();
				int count_resultarrayname = IBReceiver_Amazon.ResultArrayNameVector.size();
				int allcount = count_resultarrayname + count_resultname;

				debug("iFinished.run(0x001)->count_resultname:"+count_resultname);
				debug("iFinished.run(0x001)->count_resultarrayname:"+count_resultarrayname);
				debug("iFinished.run(0x001)->allcount:"+allcount);

				String[][] resultstrings = null;

				if(IBReceiver_Amazon.ResultStringVector.get(0).equals("RESULT_OK")){
					debug("result OK");
					resultstrings = new String[allcount][];

					for (int i = 0; i < count_resultname; i++){
						resultstrings[i] = new String[1];
						resultstrings[i][0] = IBReceiver_Amazon.ResultStringVector.get(i+1);
					}
					for (int i = count_resultname; i < allcount; i++) {
						resultstrings[i] = IBReceiver_Amazon.ResultStringArrayVector.get(i-count_resultname);
					}

					Amazon_ProductPrice1 = Function.getStringByName(
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("NAMES")], 
					"ProductPrice1");
					Amazon_ProductTitle1 = Function.getStringByName(
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("NAMES")], 
					"ProductTitle1");
					Amazon_ProductPrice2 = Function.getStringByName(
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("NAMES")], 
					"ProductPrice2");
					Amazon_ProductTitle2 = Function.getStringByName(
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("NAMES")], 
					"ProductTitle2");
					Amazon_ProductPrice3 = Function.getStringByName(
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("NAMES")], 
					"ProductPrice3");
					Amazon_ProductTitle3 = Function.getStringByName(
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("NAMES")], 
					"ProductTitle3");
					Amazon_ProductPrice4 = Function.getStringByName(
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("NAMES")], 
					"ProductPrice4");
					Amazon_ProductTitle4 = Function.getStringByName(
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("OUTPUTS")], 
							resultstrings[IBReceiver_Amazon.getArrayIndexfromName("NAMES")], 
					"ProductTitle4");
					mHandler.sendEmptyMessage(WA_Amazon_SUCCEED);
				}else if(IBReceiver_Amazon.ResultStringVector.get(0).equals("RESULT_CANCELED")){
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
					mHandler.sendEmptyMessage(WA_Amazon_FAILED);

				}else 
					mHandler.sendEmptyMessage(WA_Amazon_FAILED);
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
						String title = Amazon_ProductTitle1;
						String isbn = "";
						String price = Amazon_ProductPrice1;

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
						String title = Amazon_ProductTitle2;
						String isbn = "";
						String price = Amazon_ProductPrice2;

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
	
	public void prepareBookShoppingAdd3(){
		threadBookShoppingAdd3 = new Thread(new Runnable() {
			public void run() {
				replyBookShoppingAdd3 = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.BookDatabaseService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x66686601;
					public void onServiceConnected(ComponentName name,IBinder service) {
						debug("BookShoppingAdd Service connected: "+ name.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						String command = "ADD";
						String title = Amazon_ProductTitle3;
						String isbn = "";
						String price = Amazon_ProductPrice3;

						bundle.putString("COMMAND",command);
						bundle.putString("TITLE",title);
						bundle.putString("ISBN",isbn);
						bundle.putString("PRICE",price);

						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyBookShoppingAdd3, 0);
						} catch (RemoteException ex) {
							debug("BookShoppingSummary Service Remote exception when calling service:"+ex.toString());
							res = false;
						}

						if (res)
							mHandler.sendEmptyMessage(MS_BookShoppingAdd3_SUCCEED);
						else
							mHandler.sendEmptyMessage(MS_BookShoppingAdd3_FAILED);
					}
					public void onServiceDisconnected(ComponentName name) {
						debug("BookShoppingSummary Service disconnected: "+ name.flattenToShortString());		
					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("BookShoppingSummary Service could not be connected ");
					mHandler.sendEmptyMessage(MS_BookShoppingAdd3_FAILED);
				}
			}
		});

	}
	
	public void prepareBookShoppingAdd4(){
		threadBookShoppingAdd4 = new Thread(new Runnable() {
			public void run() {
				replyBookShoppingAdd4 = Parcel.obtain();
				Intent i = new Intent("com.prach.mashup.BookDatabaseService");
				boolean isConnected = androidContext.bindService(i,new ServiceConnection(){
					final int serviceCode = 0x66686601;
					public void onServiceConnected(ComponentName name,IBinder service) {
						debug("BookShoppingAdd Service connected: "+ name.flattenToShortString());

						Parcel data = Parcel.obtain();
						Bundle bundle = new Bundle();

						String command = "ADD";
						String title = Amazon_ProductTitle4;
						String isbn = "";
						String price = Amazon_ProductPrice4;

						bundle.putString("COMMAND",command);
						bundle.putString("TITLE",title);
						bundle.putString("ISBN",isbn);
						bundle.putString("PRICE",price);

						data.writeBundle(bundle);
						boolean res = false;
						try {
							res = service.transact(serviceCode, data,replyBookShoppingAdd4, 0);
						} catch (RemoteException ex) {
							debug("BookShoppingSummary Service Remote exception when calling service:"+ex.toString());
							res = false;
						}

						if (res)
							mHandler.sendEmptyMessage(MS_BookShoppingAdd4_SUCCEED);
						else
							mHandler.sendEmptyMessage(MS_BookShoppingAdd4_FAILED);
					}
					public void onServiceDisconnected(ComponentName name) {
						debug("BookShoppingSummary Service disconnected: "+ name.flattenToShortString());		
					}
				}, Context.BIND_AUTO_CREATE);

				if (!isConnected) {
					debug("BookShoppingSummary Service could not be connected ");
					mHandler.sendEmptyMessage(MS_BookShoppingAdd4_FAILED);
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
		"	<value>AmazonPrice.output.TITLE_1</value>\n"+
		"	<name>Price1</name>\n"+
		"	<value>AmazonPrice.output.PRICE_1</value>\n"+
		"	<name>Title2</name>\n"+
		"	<value>AmazonPrice.output.TITLE_2</value>\n"+
		"	<name>Price2</name>\n"+
		"	<value>AmazonPrice.output.PRICE_2</value>\n"+
		"	<name>Title3</name>\n"+
		"	<value>AmazonPrice.output.TITLE_3</value>\n"+
		"	<name>Price3</name>\n"+
		"	<value>AmazonPrice.output.PRICE_3</value>\n"+
		"	<name>Title4</name>\n"+
		"	<value>AmazonPrice.output.TITLE_4</value>\n"+
		"	<name>Price4</name>\n"+
		"	<value>AmazonPrice.output.PRICE_4</value>\n"+
		"	<name>Status1</name>\n"+
		"	<value>BookShoppingAdd1.output.STATUS_1</value>\n"+
		"	<name>Status2</name>\n"+
		"	<value>BookShoppingAdd2.output.STATUS_2</value>\n"+
		"	<name>Status3</name>\n"+
		"	<value>BookShoppingAdd3.output.STATUS_3</value>\n"+
		"	<name>Status4</name>\n"+
		"	<value>BookShoppingAdd4.output.STATUS_4</value>\n"+
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
