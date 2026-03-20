package com.orange.player;

import android.app.Activity;
import android.view.ViewGroup;
import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PlayerSettingsManager;
import com.orange.playerlibrary.PlayerConstants;
import com.orange.playerlibrary.VideoEventManager;
import com.orange.playerlibrary.tool.DanmakuItem;
import com.uaoanlao.tv.Screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import android.view.View;
/**
* 播放器管理器
* 封装播放器的创建、播放控制、生命周期管理等核心操作
* 简化上层调用，统一管理播放器状态
*/
public class VideoPlayerManager {
	
	private static volatile VideoPlayerManager instance;
	
	private Activity mActivity; // 关联的Activity上下文
	private OrangevideoView mVideoView; // 播放器核心视图
	private OrangeVideoController mVideoController; // 播放器控制器
	private ViewGroup mParentContainer; // 播放器父容器（用于添加到界面）
	private String urlimage="";//视频封面Url，投屏用
	private final OrangeInterFace face=new OrangeInterFace();
	
	private static String TAG="VideoPlayerManager";
	/**
	* 单例模式获取管理器实例
	* 确保全局唯一的播放器管理对象
	* @return 播放器管理器实例
	*/
	public static VideoPlayerManager getInstance() {
		if (instance == null) {
			synchronized (VideoPlayerManager.class) {
				if (instance == null) {
					instance = new VideoPlayerManager();
				}
			}
		}
		return instance;
	}
	
	
	/**
	* 初始化播放器
	* 创建播放器视图和控制器，并关联到指定的父容器
	* @param activity 关联的Activity（用于生命周期管理）
	* @param parent 播放器要添加到的父布局容器
	*/
	public void init(Activity activity, ViewGroup parent) {
		if (activity == null || parent == null) {
			throw new IllegalArgumentException("Activity和父容器不能为空");
		}
		release(); // 初始化前先释放已有资源，避免内存泄漏
		
		this.mActivity = activity;
		this.mParentContainer = parent;
		
		// 创建播放器视图
		this.mVideoView = new OrangevideoView(activity);
		this.mVideoController = new OrangeVideoController(activity, this.mVideoView);
		this.mVideoController.setBottomProgress(false); //设置显示底部进度条
		this.mVideoController.setDoubleTapTogglePlayEnabled(false);
		this.mVideoController.setPreViewEnabled(true);//是否开启拖动预览小窗
		
		// 设置默认内核（使用实例方法）
		PlayerSettingsManager.getInstance(activity).setPlayerEngine(PlayerConstants.ENGINE_DEFAULT);
		
		this.mVideoView.setVideoController(this.mVideoController); //设置控制器
		// 将播放器添加到父容器
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
		ViewGroup.LayoutParams.MATCH_PARENT,
		ViewGroup.LayoutParams.MATCH_PARENT
		);
		this.mParentContainer.addView(this.mVideoView, params);
		// 初始化控制器组件（默认非直播模式）
		this.mVideoController.addDefaultControlComponent("视频标题", false);
	}
	
	/**
	* 设置内核
	* @param videoUrl 视频播放地址
	* @param videoTitle 视频标题（用于控制器显示）
	*/
	public void selectEngine(String engine) {
		//需要在start(视频播放前)前切换才有效果
        if (mActivity != null) {
            PlayerSettingsManager.getInstance(mActivity).setPlayerEngine(engine);
        }
	}
    
	/**
	* 设置视频源
	* @param videoUrl 视频播放地址
	* @param videoTitle 视频标题（用于控制器显示）
	*/
	public void setVideoSource(String videoUrl, String videoTitle) {
		checkInitState(); // 检查是否已初始化
		
		this.mVideoView.setUrl(videoUrl); // 设置播放地址
		this.mVideoController.setVideoTitle(videoTitle); // 设置视频标题
	}
	
	
	/**
	* 设置视频源
	* @param videoUrl 视频播放地址
	* @param head 请求头
	* @param videoTitle 视频标题（用于控制器显示）
	*/
	public void setVideoSource(String videoUrl,Map<String, String>head ,String videoTitle) {
		
		checkInitState(); // 检查是否已初始化
		
		this.mVideoView.setUrl(videoUrl,head); // 设置播放地址
		this.mVideoController.setVideoTitle(videoTitle); // 设置视频标题
	}
	
	/**
	* 设置播放列表（用于多集视频切换）
	* @param videoList 视频列表，每个元素为包含"name"(标题)和"url"(地址)的HashMap
	*/
	public void setVideoList(ArrayList<HashMap<String, Object>> videoList) {
		checkInitState();
		this.mVideoController.setVideoList(videoList);
	}
	
	//设置视频封面
	public void setVideoImage(String url)
	{
		this.urlimage=url;
	}
	
	/**
	* 开始播放视频
	*/
	public void start() {
		checkInitState();
		//清空现有弹幕
		clearDanmakus();
		if (!this.mVideoView.isPlaying()) {
			this.mVideoView.start();
		}
	}
	
	/**
	*嗅探播放
	*/
	public void startSniffing() {
		this.mVideoView.startSniffing();
	}
	//设置加载动画1-28
	public void setLoading(int i)
	{
		this.mVideoController.setLoading(i);
	}
	
	/**
	* 暂停播放
	*/
	public void pause() {
		checkInitState();
		if (this.mVideoView.isPlaying()) {
			this.mVideoView.pause();
		}
	}
	
	
	/**
	* 停止播放并重置播放器
	*/
	public void stop() {
		checkInitState();
		clearDanmakus();
		this.mVideoView.pause();
		this.mVideoView.release(); // 释放播放器资源
	}
	
	
	/**
	* 跳转至指定进度
	* @param position 目标进度（毫秒）
	*/
	public void seekTo(int position) {
		checkInitState();
		if (position >= 0 && position <= this.mVideoView.getDuration()) {
			this.mVideoView.seekTo(position);
		}
	}
	
	
	/**
	* 设置播放速度
	* @param speed 播放速度（1.0f为正常速度，0.5f为半速，2.0f为2倍速等）
	*/
	public void setPlaybackSpeed(float speed) {
		checkInitState();
		if (speed > 0) {
			this.mVideoView.setSpeed(speed);
			OrangevideoView.setSpeeds(speed); // 保存速度设置
		}
	}
	
	
	/**
	* 切换全屏/非全屏模式
	*/
	public void toggleFullScreen() {
		checkInitState();
		if (this.mVideoView.isFullScreen()) {
			this.mVideoController.stopFullScreen(); // 退出全屏
		} else {
			this.mVideoController.startFullScreen(); // 进入全屏
		}
	}
	/**
	* 是否全屏
	*/
    
	public boolean isFullScreen() {
		checkInitState();
		return this.mVideoView.isFullScreen();
	}
    
	/**
	* 设置音量（左右声道同时设置）
	* @param volume 音量值（0-100），转换为0-1范围后设置给左右声道
	*/
	public void setVolume(int volume) {
		checkInitState();
		if (volume >= 0 && volume <= 100) {
			float volumeRatio = volume / 100.0f; // 转换为0-1范围
			// 同时设置左右声道音量（满足方法参数要求）
			this.mVideoView.setVolume(volumeRatio, volumeRatio);
		}
	}
	
	
	/**
	* 获取当前播放进度
	* @return 当前进度（毫秒）
	*/
	public long getCurrentPosition() {
		checkInitState();
		return this.mVideoView.getCurrentPosition();
	}
	
	
	/**
	* 获取视频总时长
	* @return 总时长（毫秒），直播视频返回0
	*/
	public long getDuration() {
		checkInitState();
		return this.mVideoView.getDuration();
	}
	
	
	/**
	* 设置播放完成监听
	* @param listener 播放完成回调
	*/
	public void setOnPlayCompleteListener(OrangevideoView.OnPlayComplete listener) {
		checkInitState();
		this.mVideoView.setOnPlayComplete(listener);
	}
	
	
	/**
	* 设置进度更新监听（每秒回调一次）
	* @param listener 进度回调
	*/
	public void setOnProgressListener(OrangevideoView.OnProgressListener listener) {
		checkInitState();
		OrangevideoView.setOnProgressListener(listener);
	}
	
	
	/**
	* 处理Activity生命周期 - 暂停时调用
	* 在Activity的onPause中调用，用于暂停播放
	*/
	public void onPause() {
		if (this.mVideoView != null && this.mVideoView.isPlaying()) {
			this.mVideoView.onVideoPause();
		}
	}
	
	
	/**
	* 处理Activity生命周期 - 恢复时调用
	* 在Activity的onResume中调用，用于恢复播放
	*/
	public void onVideoResume() {
		
		this.mVideoView.resume();
		
	}
	
	
	/**
	* 释放播放器资源
	* 在Activity的onDestroy中调用，避免内存泄漏
	*/
	public void release() {
		if (this.mVideoView != null) {
			this.mVideoView.release();
			this.mVideoView = null;
		}
		if (this.mVideoController != null) {
			this.mVideoController = null;
		}
		if (this.mParentContainer != null) {
			this.mParentContainer.removeAllViews(); // 从父容器移除播放器
			this.mParentContainer = null;
		}
		this.mActivity = null;
	}
	
	
	/**
	* 检查播放器是否已初始化
	* 未初始化时抛出异常，避免空指针
	*/
	private void checkInitState() {
		if (this.mVideoView == null || this.mVideoController == null) {
			throw new IllegalStateException("播放器未初始化，请先调用init()方法");
		}
	}
	
	
	/**
	* 获取当前播放器实例（谨慎使用，建议通过管理器方法操作）
	* @return OrangevideoView实例
	*/
	public OrangevideoView getVideoView() {
		return this.mVideoView;
	}
	
	
	/**
	* 获取当前控制器实例（谨慎使用，建议通过管理器方法操作）
	* @return OrangeVideoController实例
	*/
	public OrangeVideoController getVideoController() {
		return this.mVideoController;
	}
	
	//开启投屏
	public void openScreenTv()
	{
		
		face.setScreenTvVisibility(true);
		
		face.setScreenTvOnClickListener(new OrangeInterFace.ScreenTvOnClickListener() {
			@Override
			public void onClick(View v) {
				ScreenTvOnClickListener();
			}
		});
	}
	
	public void ScreenTvOnClickListener()
	{
		new Screen().setStaerActivity(mActivity)
		.setName(this.mVideoView.getitle())
		.setUrl(this.mVideoView.getUrl())//视频地址
		.setImageUrl(this.urlimage)//视频封面
		.show();
	}
	
	//显示下一集按钮
	public void setPlayNextVisibility(boolean is)
	{
		face.setPlayNextVisibility(is);
	}
	//显示选集按钮
	public void setPlayListVisibility(boolean is)
	{
		face.setPlayListVisibility(is);
	}
	//显示竖屏全屏按钮
	public void setFullScreenVertical(boolean is)
	{
		face.setFullScreenVertical(is);
	}
	//添加单个集数，是否独立视频
	public synchronized void addVideo(String tile,String url,boolean is)
	{
		
		getVideoController().addVideo(tile,url,is);
	}
	
	
	
	/**
	* 从单个JSON对象中解析参数，调用addVideo方法
	* JSON格式示例：{"name":"视频名称","url":"http://xxx","isSpecial":true}
	*/
	public void addVideoFromJson(String jsonObjStr) {
		try {
			// 1. 解析JSON字符串为JSONObject
			JSONObject jsonObj = new JSONObject(jsonObjStr);
			
			// 2. 提取三个参数（注意：字段名需与JSON中保持一致，此处为示例）
			String name = jsonObj.getString("name"); // 对应str（"name"键）
			String url = jsonObj.getString("url");   // 对应str2（"url"键）
			boolean isSpecial = jsonObj.getBoolean("isSpecial"); // 对应z（"isSpecial"键）
			
			// 3. 调用原addVideo方法
			addVideo(name, url, isSpecial);
			
		} catch (JSONException e) {
			e.printStackTrace();
			// 处理JSON格式错误、字段缺失等异常
			debug("解析JSON失败：" + e.getMessage());
		}
	}
	
	
	/**
	* 从JSON数组中批量解析参数，批量调用addVideo方法
	* JSON数组示例：[{"name":"视频1","url":"http://x1","isSpecial":true}, {...}]
	*/
	public void addVideosFromJsonArray(String jsonArrayStr) {
		try {
			JSONArray jsonArray = new JSONArray(jsonArrayStr);
			for (int i = 0; i < jsonArray.length(); i++) {
				// 逐个解析数组中的对象，调用单个添加方法
				addVideoFromJson(jsonArray.getJSONObject(i).toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
			debug("解析JSON数组失败：" + e.getMessage());
		}
	}
	
	public void addVideosFromJsonArray(JSONArray jsonArray) {
		try {
			for (int i = 0; i < jsonArray.length(); i++) {
				// 逐个解析数组中的对象，调用单个添加方法
				addVideoFromJson(jsonArray.getJSONObject(i).toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
			debug("解析JSON数组失败：" + e.getMessage());
		}
	}
	
	//事件控制器
	public VideoEventManager getVideoEventManager()
	{
		return getVideoController().getVideoEventManager();
	}
	//添加一个弹幕
	
	
	
	/**
	* 将十六进制颜色字符串转换为整数颜色值
	* 支持格式：#RGB、#RGBA、#RRGGBB、#RRGGBBAA（如 #f00、#f00f、#ff0000、#ff0000ff）
	* @param colorStr 十六进制颜色字符串（必须以 # 开头）
	* @return 整数颜色值（如 #ff0000 对应 -65536），转换失败返回默认颜色（白色）
	*/
	private int parseColorString(String colorStr) {
		try {
			// 处理简写格式（如 #f00 → #ff0000，#f00f → #ff0000ff）
			if (colorStr.length() == 4) { // #RGB → 扩展为 #RRGGBB
				StringBuilder sb = new StringBuilder("#");
				for (int i = 1; i < 4; i++) {
					char c = colorStr.charAt(i);
					sb.append(c).append(c); // 重复字符（如 f → ff）
				}
				colorStr = sb.toString();
			} else if (colorStr.length() == 5) { // #RGBA → 扩展为 #RRGGBBAA
				StringBuilder sb = new StringBuilder("#");
				for (int i = 1; i < 5; i++) {
					char c = colorStr.charAt(i);
					sb.append(c).append(c);
				}
				colorStr = sb.toString();
			}
			// 使用Android原生方法转换为int颜色值
			return Color.parseColor(colorStr);
		} catch (Exception e) {
			// 处理无效格式（如非#开头、字符错误等），返回默认颜色（白色）
			e.printStackTrace();
			return Color.WHITE; // 默认白色
		}
	}
	
	
	/**
	* 从JSON字符串解析弹幕列表（支持颜色字符串格式：#ff0000）
	* JSON格式示例：
	* [
	*   {"text":"测试弹幕1","color":"#ff0000","timestamp":0,"isSelf":false},
	*   {"text":"中间点弹幕","color":"#ffff00","timestamp":200000,"isSelf":true}
	* ]
	*/
	
	
	public void setDanmuListFromJson(String jsonStr) {
		try {
			JSONArray jsonArray = new JSONArray(jsonStr);
			List<DanmakuItem> danmakuList = new ArrayList<>();
			
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject danmakuObj = jsonArray.getJSONObject(i);
				
				// 解析弹幕属性
				String text = danmakuObj.getString("text");
				String colorStr = danmakuObj.getString("color"); // 颜色字符串（如#ff0000）
				long timestamp = danmakuObj.getLong("timestamp");
				boolean isSelf = danmakuObj.getBoolean("isSelf");
				
				// 将颜色字符串转换为int值
				int color = parseColorString(colorStr);
				
				// 创建DanmakuItem并添加到列表
				danmakuList.add(new DanmakuItem(text, color, timestamp, isSelf));
			}
			
			// 调用原方法设置弹幕列表
			getVideoEventManager().setDanmuList(danmakuList);
			
			getVideoEventManager().setAllDanmakus();
		} catch (JSONException e) {
			e.printStackTrace();
			// 处理JSON解析异常（如字段缺失、格式错误）
		}
	}
	
	public boolean setDanmuListFromJson(JSONArray jsonArray) {
		try {
			
			List<DanmakuItem> danmakuList = new ArrayList<>();
			
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject danmakuObj = jsonArray.getJSONObject(i);
				
				// 解析弹幕属性
				String text = danmakuObj.getString("text");
				String colorStr = danmakuObj.getString("color"); // 颜色字符串（如#ff0000）
				long timestamp = danmakuObj.getLong("timestamp");
				boolean isSelf = danmakuObj.getBoolean("isSelf");
				
				// 将颜色字符串转换为int值
				int color = parseColorString(colorStr);
				
				// 创建DanmakuItem并添加到列表
				danmakuList.add(new DanmakuItem(text, color, timestamp, isSelf));
			}
			
			// 调用原方法设置弹幕列表
			getVideoEventManager().setDanmuList(danmakuList);
			
			//手动全部设置弹幕
			getVideoEventManager().setAllDanmakus();
			return true;
		} catch (JSONException e) {
			e.printStackTrace();
			debug("设置弹幕出错:" + e);
			return false;
		}
	}
	
	
	
	// 设置弹幕发送功能
	public void sendDanmu(String danmu,int color,boolean issf)
	{
		getVideoEventManager().sendDanmu(danmu,color, issf);
	}
	// 设置弹幕发送功能
	public void sendDanmu(String danmu,String colorStr,boolean issf)
	{
		int color = parseColorString(colorStr);
		getVideoEventManager().sendDanmu(danmu,color, issf);
	}
	
	
	//清空弹幕
	public void clearDanmakus()
	{
		try
		{
			getVideoEventManager().getDanmuView().clearDanmakus();
		}catch(Exception e)
		{
			
		}
	}
	
	public static void debug(Object obj)
	{
		android.util.Log.d(TAG, String.valueOf(obj));
	}
}