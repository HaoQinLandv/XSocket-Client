# XSocket客户端。

* 绑定服务端IP与端口
```java
    IBlockingConnection nbc = new BlockingConnection(this.serverip, this.serverport);
```
* 设置超时
```java
    nbc.setConnectionTimeoutMillis(this.conntimeout*1000);
```

* 代码实例如下：
```java
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
```
* 发送数据到服务端，采用json+ascii格式

   * 申请json
```java
       JSONObject js=new JSONObject();
		js.put("method", "readcard");
		js.put("data", input);
```
   * 获取连接
```java
       nbc=getBc();
```     
   * 发送数据
```java
       nbc.write(js.toJSONString()+ETX);
```
   * 接收服务端数据
 ```java  
       String res = nbc.readStringByDelimiter(ETX);
```        


发送数据到服务端的代码示例：
```java
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
```
