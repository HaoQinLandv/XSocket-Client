/**
 * 
 */
package com.foxhis.itf.doorcard;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;

import com.alibaba.fastjson.JSONObject;
import com.foxhis.itf.handler.AbstractDoorCardHandler;
import com.foxhis.itf.handler.IFoxItfLocalOperate;
import com.foxhis.pms.biz.doorcardlink.DoorcardLinkBiz;

/**
 * <p>
 * Project Name: 门锁接口的通用客户端
 * <br>
 * Description:
 *  <p> 目前针对jre8不兼容门锁提供的dll文件的时候，采用该方案
 *  <p> 
 *         a.  
 *         b. 
 *         c. 
 * <br>
 * File Name: DoorCard_BtLock57_GEN.java
 * <br>
 * Copyright: Copyright (C) 2017 All Rights Reserved.
 * <br>
 * Company: 杭州XXX
 * <br>
 * @author tq8
 * @createTime: 2018-04-26 14:16:32  
 * @version: v1.0
 * 
 *       Date              Author      Version     Description
 * ------------------------------------------------------------------
 * 2018-04-26 14:16:32     |tq8　　|v1.0        |Create
 * 
 */
public class DoorCard_GENClient extends AbstractDoorCardHandler {

	//静态加载log4j配置
	static
	{
		String resource = "/com/foxhis/i18n/config/log.properties";
		URL configFileResource = DoorCard_GENClient.class.getResource(resource);
		PropertyConfigurator.configure(configFileResource);
	}
    //log4j管理日志
	private static final Logger logger = Logger.getLogger("doorlogger");
	//约定的json结束位
	private static final String ETX = "\3";
	//超时连接，默认为10秒
	private int conntimeout=10;
	//idle超时，默认也是10秒
	private int idletimeout=10;
	//服务端ip
	private String serverip="127.0.0.1";
	//服务端端口
	private int serverport=6666;
	//是否需要管理序列号
	private boolean isdoorcardlink=false;
	//设置连接对象
	//private IBlockingConnection nbc;

	public String getItfVersion() {
		return "1.0";
	}

	public void initialize(IFoxItfLocalOperate lop) {
		super.initialize(lop);
		this.conntimeout = getItfProperty("doorcard.btlock.conntimeout", Integer.class).intValue();
		this.idletimeout = getItfProperty("doorcard.btlock.idletimeout", Integer.class).intValue();
		this.serverip = getItfProperty("doorcard.btlock.serverip");
		this.serverport = getItfProperty("doorcard.btlock.serverport",Integer.class).intValue();
        this.isdoorcardlink = getItfProperty("doorcard.btlock.isdoorcardlink",Boolean.class);
		setInitialized(true);
		lop.operate("InitRoomno_Check", new Object[0]);
		
	}
	
	/**
	 * 获取BlockingConnection，并设置超时conntimeout*1000
	 * @return
	 * @throws IOException
	 */
	public IBlockingConnection getBc() throws IOException{
		
		IBlockingConnection nbc = new BlockingConnection(this.serverip, this.serverport);
		nbc.setConnectionTimeoutMillis(this.conntimeout*1000);
		//nbc.setIdleTimeoutMillis(this.idletimeout*1000);
		logger.info(MessageFormat.format("连接服务端成功，参数分别是连接超时时间{0},idle超时为{1},服务器ip{2},服务器端口{3}", 
				new Object[]{conntimeout,idletimeout,serverip,serverport}));

		return nbc;
	}

	public void deinitialize() {
		super.deinitialize();
	}

	public String initroomno(String DoorID) {
		locOperate.operate("InitRoomno_Check", new Object[0]);
		String roomno = (String) this.locOperate.operate("DOORCARD_initroomno", new Object[] { DoorID });
		return roomno;
	}

	public String initlockno(String roomno) {
		locOperate.operate("InitRoomno_Check", new Object[0]);
		String DoorID = (String) this.locOperate.operate("DOORCARD_initlockno", new Object[] { roomno });
		return DoorID;
	}


	public Map<String, Object> eraseCardInternal(Map<String, Object> input) throws Exception {
		Map<String, Object> retMap = new HashMap<String, Object>();
		JSONObject js=new JSONObject();
		js.put("method", "erasecard");
		js.put("data", input);
		IBlockingConnection nbc=null;
		
		logger.info("开始销卡，其参数为"+js.toJSONString());
		try {
			nbc=getBc();
			nbc.write(js.toJSONString()+ETX);
			String res = nbc.readStringByDelimiter(ETX);
			logger.info("销卡返回，其参数为"+res);
			retMap = JSONObject.parseObject(res);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("接口发送服务端异常",e);
			retMap.put("result", false);
			retMap.put("msg", "连接服务端异常，请检查服务端是否启动");
		}
		finally {
			if(nbc!=null){
				nbc.close();
			}
		}
		return retMap;

	}

	public Map<String, Object> readCardInternal(Map<String, Object> input) throws Exception {
		Map<String, Object> retmap = new HashMap<String, Object>();
		JSONObject js=new JSONObject();
		js.put("method", "readcard");
		js.put("data", input);
		logger.info("开始读卡，其参数为"+js.toJSONString());
		
		IBlockingConnection nbc=null;
		try {
			nbc=getBc();
			nbc.write(js.toJSONString()+ETX);
			String res = nbc.readStringByDelimiter(ETX);
			logger.info("读卡返回，其参数为"+res);
			retmap = JSONObject.parseObject(res);
			retmap.put("roomno", initroomno((String)retmap.get("roomno")));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("接口发送服务端异常",e);
			retmap.put("result", false);
			retmap.put("msg", "连接服务端异常，请检查服务端是否启动");
		}
		finally {
			if(nbc!=null){
				nbc.close();
			}
		}
		return retmap;
	}

	
	/**
	 * 实际的写卡，涉及到序列号的问题，需要服务端返回cardno1,cardno2作为客人序列号返回
	 * input在原由的基础上加了，如果是复制卡加了cardno1,cardno2,两个key参数，lockno参数
	 */
	public Map<String, Object> writeCardInternal(Map<String, Object> input) throws Exception {

		Map<String, Object> retmap = new HashMap<String, Object>();
		String roomno = (String)input.get("roomno");
		String accnt = (String)input.get("accnt");
		String cardtype = (String)input.get("cardtype");
		if(this.isdoorcardlink && "copy".equals(cardtype))
		{
			Map<String,Object> map = getCommonSeriNO(roomno, accnt);
			if(map!=null && !map.isEmpty())//如果有数据则，将客人序列号传入服务端
			{
				input.put("cardno1", map.get("cardno1"));
				input.put("cardno2", map.get("cardno2"));
			}
			logger.info("用doorcardlink管理序列号，且是复制卡时获取的序列号:"+map);
		}
		input.put("roomno", initlockno(roomno));
		input.put("roomno1",roomno);
		JSONObject js=new JSONObject();
		js.put("method", "writecard");
		js.put("data", input);
		logger.info("开始写卡，其参数为"+js.toJSONString());
		IBlockingConnection nbc=null;

		try {
			nbc=getBc();
			nbc.write(js.toJSONString()+ETX);
			String res = nbc.readStringByDelimiter(ETX);
			logger.info("写卡，其参数为"+res);
			retmap = JSONObject.parseObject(res);	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("接口发送服务端异常",e);
			retmap.put("result", false);
			retmap.put("msg", "连接服务端异常，请检查服务端是否启动");
		}
		finally {
			if(nbc!=null){
				nbc.close();
			}
		}
		if(this.isdoorcardlink){
			if((Boolean)retmap.get("result") && retmap.containsKey("cardno1") && retmap.containsKey("cardno2")){
				int cardno1 = (Integer)retmap.get("cardno1");
				int cardno2 = (Integer)retmap.get("cardno2");
				writeCommonSeriNO(roomno,accnt,cardtype,cardno1,cardno2);
			}
			else{
				logger.info("接收返回值result不是true或者无cardno1与cardno2，无法写入doorcardlink");
			}
		}		
		return retmap;
	}
	
	/**
	 * 传入公共序列号到doorcardlink
	 * @param roomno
	 * @param accnt
	 * @param lockno
	 * @param cardtype
	 * @param name
	 * @param cardno1
	 * @param cardno2
	 */
	public void writeCommonSeriNO(String roomno, String accnt,String cardtype, int cardno1, int cardno2) {
		DoorcardLinkBiz biz = (DoorcardLinkBiz) getFacadeRemote(DoorcardLinkBiz.class);
		String hotelid = this.locOperate.getProperty("foxitf.hotelid");
		Map<Object, Object> info = new HashMap<Object, Object>();
		info.put("hotelid_door", hotelid);
		info.put("empno_door", "FOX");
		info.put("roomno_door", roomno);
		info.put("accnt_door", accnt);
		info.put("id_door", roomno);
		info.put("cardtype_door", cardtype);
		info.put("arr_door", "1999-01-01 00:00:00");
		info.put("dep_door", "1999-01-01 00:00:00");
		info.put("name_door", "");
		info.put("cardno1_door", Integer.valueOf(cardno1));
		info.put("cardno2_door", Integer.valueOf(cardno2));
		info.put("remark_door", "");
		info.put("exts2_door", "F");
		info.put("oldaccnt", "static");
		Map<Object, Object> res = biz.crateDoorcardLink(info);
		logger.info("存入公共序列号doorcardlink反馈："+res);
	}
	
	/***
	 * 获取公共序列号
	 * @param roomno
	 * @param accnt
	 * @return map
	 */
	public Map<String, Object> getCommonSeriNO(String roomno, String accnt){
		Map<String, Object> retmap=new HashMap<String, Object>();
		DoorcardLinkBiz biz = (DoorcardLinkBiz) getFacadeRemote(DoorcardLinkBiz.class);
		String hotelid = this.locOperate.getProperty("foxitf.hotelid");
		Map<Object, Object> copyinfo = biz.getDoorcardLinkWithSQL(hotelid," and roomno = '" + roomno + "' and accnt='" + accnt + "'");
		logger.info("获取公共序列号map:" + copyinfo);
		//没有该房间该帐号，序列号返回空
		if ((copyinfo.size() <= 3) && (copyinfo.containsKey("accnt"))) {

		}
		else{
			int cardno1 = Integer.parseInt((String)copyinfo.get("cardno1_door"));
			int cardno2 = Integer.parseInt((String)copyinfo.get("cardno2_door"));
			retmap.put("cardno1", cardno1);
			retmap.put("cardno2", cardno2);
		}
		return retmap;
		
	}
	
	
	
	public static void main(String[] args) throws Exception {
		DoorCard_GENClient doorcard =new DoorCard_GENClient();
		Map<String, Object> retmap = new HashMap<String, Object>();
		retmap.put("roomno", "1101");
		retmap.put("msg", "测试销卡");
		System.out.println(doorcard.eraseCardInternal(retmap));
	
	}

}
