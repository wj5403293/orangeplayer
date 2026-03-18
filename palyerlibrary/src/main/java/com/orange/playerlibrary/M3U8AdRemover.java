package com.orange.playerlibrary;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * M3U8去广告处理器
 * 
 * 功能：
 * 1. 检测m3u8中的广告片段
 * 2. 移除广告片段生成干净的m3u8
 * 3. 缓存处理结果避免重复请求
 * 4. 请求失败时返回原始URL
 */
public class M3U8AdRemover {
    
    private static final String TAG = "M3U8AdRemover";
    private static final String CACHE_DIR = "m3u8_cache";
    private static final String CACHE_INDEX_FILE = "cache_index.txt";
    
    private final Context mContext;
    private final File mCacheDir;
    private final ExecutorService mExecutor;
    
    // TS白名单（实例级别，重启失效）
    private Set<String> mTsWhitelist = new HashSet<>();
    
    // 广告检测回调
    public interface Callback {
        void onResult(String playUrl, boolean isLocalFile, int adSegmentsRemoved);
        void onError(String originalUrl, Exception e);
    }
    
    public M3U8AdRemover(Context context) {
        mContext = context.getApplicationContext();
        mCacheDir = new File(context.getCacheDir(), CACHE_DIR);
        if (!mCacheDir.exists()) {
            mCacheDir.mkdirs();
        }
        mExecutor = Executors.newSingleThreadExecutor();
    }
    
    // ===== TS白名单操作 =====
    
    /**
     * 添加TS片段到白名单
     * 白名单中的片段不会被当作广告过滤
     * 
     * @param tsUrl TS片段的完整URL或URL关键字（包含该关键字的URL都不会被过滤）
     */
    public void addToWhitelist(String tsUrl) {
        if (tsUrl != null && !tsUrl.isEmpty()) {
            mTsWhitelist.add(tsUrl);
            Log.d(TAG, "Added to whitelist: " + tsUrl);
        }
    }
    
    /**
     * 批量添加TS片段到白名单
     * 
     * @param tsUrls TS片段URL列表
     */
    public void addToWhitelist(List<String> tsUrls) {
        if (tsUrls != null) {
            for (String url : tsUrls) {
                addToWhitelist(url);
            }
        }
    }
    
    /**
     * 从白名单移除TS片段
     * 
     * @param tsUrl 要移除的URL
     */
    public void removeFromWhitelist(String tsUrl) {
        if (tsUrl != null) {
            mTsWhitelist.remove(tsUrl);
            Log.d(TAG, "Removed from whitelist: " + tsUrl);
        }
    }
    
    /**
     * 清空白名单
     */
    public void clearWhitelist() {
        mTsWhitelist.clear();
        Log.d(TAG, "Whitelist cleared");
    }
    
    /**
     * 检查URL是否在白名单中
     * 支持完整URL匹配和关键字匹配
     * 
     * @param tsUrl 要检查的URL
     * @return 是否在白名单中
     */
    public boolean isInWhitelist(String tsUrl) {
        if (tsUrl == null || mTsWhitelist.isEmpty()) {
            return false;
        }
        
        // 完整匹配
        if (mTsWhitelist.contains(tsUrl)) {
            return true;
        }
        
        // 关键字匹配（白名单项作为子串）
        for (String whitelistItem : mTsWhitelist) {
            if (tsUrl.contains(whitelistItem)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取白名单大小
     */
    public int getWhitelistSize() {
        return mTsWhitelist.size();
    }
    
    /**
     * 处理m3u8 URL，移除广告片段
     * 
     * @param m3u8Url 原始m3u8 URL
     * @param callback 结果回调
     */
    public void processM3U8(String m3u8Url, Callback callback) {
        mExecutor.execute(() -> {
            try {
                processM3U8Internal(m3u8Url, callback);
            } catch (Exception e) {
                Log.e(TAG, "processM3U8 error", e);
                callback.onError(m3u8Url, e);
            }
        });
    }
    
    private void processM3U8Internal(String m3u8Url, Callback callback) throws Exception {
        // 1. 检查缓存
        String cacheKey = getCacheKey(m3u8Url);
        File cachedFile = new File(mCacheDir, cacheKey + ".m3u8");
        
        if (cachedFile.exists()) {
            // 检查缓存索引获取广告片段数
            int adCount = getAdCountFromCacheIndex(cacheKey);
            Log.d(TAG, "Using cached m3u8, ad segments removed: " + adCount);
            callback.onResult(cachedFile.getAbsolutePath(), true, adCount);
            return;
        }
        
        // 2. 请求m3u8内容
        String m3u8Content = fetchM3U8Content(m3u8Url);
        if (m3u8Content == null || m3u8Content.isEmpty()) {
            Log.w(TAG, "Failed to fetch m3u8 content, using original URL");
            callback.onError(m3u8Url, new Exception("Failed to fetch m3u8 content"));
            return;
        }
        
        // 2.5 检测是否为Master Playlist（嵌套m3u8）
        String subM3U8Url = extractSubM3U8Url(m3u8Content, m3u8Url);
        if (subM3U8Url != null) {
            Log.d(TAG, "Detected Master Playlist, fetching sub playlist: " + subM3U8Url);
            m3u8Content = fetchM3U8Content(subM3U8Url);
            if (m3u8Content == null || m3u8Content.isEmpty()) {
                Log.w(TAG, "Failed to fetch sub m3u8 content, using original URL");
                callback.onError(m3u8Url, new Exception("Failed to fetch sub m3u8 content"));
                return;
            }
            // 更新URL为子播放列表URL
            m3u8Url = subM3U8Url;
        }
        
        // 3. 解析并移除广告片段
        M3U8ParseResult result = parseAndRemoveAds(m3u8Content, m3u8Url);
        
        if (result.adSegmentsRemoved == 0) {
            // 没有广告，直接使用原始URL
            Log.d(TAG, "No ad segments found, using original URL");
            callback.onResult(m3u8Url, false, 0);
            return;
        }
        
        // 4. 保存处理后的m3u8文件
        String cleanedContent = result.cleanedContent;
        FileOutputStream fos = new FileOutputStream(cachedFile);
        fos.write(cleanedContent.getBytes("UTF-8"));
        fos.close();
        
        // 5. 更新缓存索引
        updateCacheIndex(cacheKey, result.adSegmentsRemoved);
        
        Log.d(TAG, "Saved cleaned m3u8 to cache, ad segments removed: " + result.adSegmentsRemoved);
        callback.onResult(cachedFile.getAbsolutePath(), true, result.adSegmentsRemoved);
        
        // 6. 清理旧缓存
        cleanOldCache();
    }
    
    /**
     * 获取m3u8内容
     */
    private String fetchM3U8Content(String m3u8Url) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(m3u8Url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP response code: " + responseCode);
                return null;
            }
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "fetchM3U8Content error", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 解析m3u8并移除广告片段
     */
    private M3U8ParseResult parseAndRemoveAds(String content, String baseUrl) {
        M3U8ParseResult result = new M3U8ParseResult();
        
        // 提取基础URL用于转换相对路径
        String baseUrlPath = extractBaseUrlPath(baseUrl);
        Log.d(TAG, "Base URL path: " + baseUrlPath);
        
        String[] lines = content.split("\n");
        StringBuilder cleaned = new StringBuilder();
        
        List<SegmentInfo> segments = new ArrayList<>();
        List<String> headerLines = new ArrayList<>();
        
        // 解析所有片段
        int currentIndex = 0;
        boolean inHeader = true;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.startsWith("#EXT-X-")) {
                if (line.startsWith("#EXT-X-TARGETDURATION") || 
                    line.startsWith("#EXT-X-MEDIA-SEQUENCE") ||
                    line.startsWith("#EXT-X-VERSION") ||
                    line.startsWith("#EXT-X-PLAYLIST-TYPE") ||
                    line.startsWith("#EXTM3U")) {
                    headerLines.add(line);
                }
                
                if (line.equals("#EXT-X-DISCONTINUITY")) {
                    // 标记流切换点
                    if (!segments.isEmpty()) {
                        segments.get(segments.size() - 1).isDiscontinuity = true;
                    }
                }
            } else if (line.startsWith("#EXTINF:")) {
                // #EXTINF 不是以 #EXT-X- 开头，需要单独处理
                inHeader = false;
                // 解析片段时长
                double duration = parseExtinfDuration(line);
                
                // 获取下一个非空行作为URL
                String segmentUrl = null;
                for (int j = i + 1; j < lines.length; j++) {
                    String nextLine = lines[j].trim();
                    if (!nextLine.isEmpty() && !nextLine.startsWith("#")) {
                        segmentUrl = nextLine;
                        break;
                    } else if (nextLine.startsWith("#EXT-X-DISCONTINUITY")) {
                        // DISCONTINUITY标记
                        break;
                    }
                }
                
                if (segmentUrl != null) {
                    SegmentInfo info = new SegmentInfo();
                    info.index = currentIndex++;
                    info.duration = duration;
                    info.url = segmentUrl;
                    info.lineNumber = i;
                    segments.add(info);
                }
            }
        }
        
        // 检测广告片段
        Log.d(TAG, "Total segments parsed: " + segments.size());
        for (int i = 0; i < Math.min(5, segments.size()); i++) {
            Log.d(TAG, "Segment " + i + ": " + segments.get(i).url + ", duration=" + segments.get(i).duration + ", isDiscontinuity=" + segments.get(i).isDiscontinuity);
        }
        
        List<SegmentInfo> adSegments = detectAdSegments(segments);
        result.adSegmentsRemoved = adSegments.size();
        
        // 构建清理后的m3u8
        // 添加header
        cleaned.append("#EXTM3U\n");
        cleaned.append("#EXT-X-VERSION:3\n");
        cleaned.append("#EXT-X-TARGETDURATION:2\n");
        
        // 添加清理后的片段
        boolean firstSegment = true;
        for (SegmentInfo segment : segments) {
            if (segment.isAd) {
                continue; // 跳过广告片段
            }
            
            if (firstSegment) {
                // 第一个片段前不需要DISCONTINUITY
                firstSegment = false;
            } else if (segment.needsDiscontinuity) {
                cleaned.append("#EXT-X-DISCONTINUITY\n");
            }
            
            // 转换为绝对URL
            String absoluteUrl = toAbsoluteUrl(segment.url, baseUrlPath);
            
            cleaned.append("#EXTINF:").append(String.format("%.6f", segment.duration)).append(",\n");
            cleaned.append(absoluteUrl).append("\n");
        }
        
        cleaned.append("#EXT-X-ENDLIST\n");
        
        result.cleanedContent = cleaned.toString();
        return result;
    }
    
    /**
     * 检测广告片段
     * 支持：开头广告、中间广告、结尾广告
     */
    private List<SegmentInfo> detectAdSegments(List<SegmentInfo> segments) {
        List<SegmentInfo> adSegments = new ArrayList<>();
        
        if (segments.size() < 2) {
            return adSegments;
        }
        
        // 统计所有路径模式的出现次数
        java.util.Map<String, Integer> pathCounts = new java.util.HashMap<>();
        for (SegmentInfo segment : segments) {
            String pathPattern = extractPathPattern(segment.url);
            pathCounts.put(pathPattern, pathCounts.getOrDefault(pathPattern, 0) + 1);
        }
        
        // 找出出现次数最多的路径模式作为主路径（正片）
        String mainPathPattern = null;
        int mainPathCount = 0;
        for (java.util.Map.Entry<String, Integer> entry : pathCounts.entrySet()) {
            if (entry.getValue() > mainPathCount) {
                mainPathPattern = entry.getKey();
                mainPathCount = entry.getValue();
            }
        }
        
        Log.d(TAG, "Path patterns: " + pathCounts.toString());
        Log.d(TAG, "Main path pattern: " + mainPathPattern + " (count=" + mainPathCount + ")");
        
        // 方法1: 按前缀长度分组检测广告
        // 统计各前缀长度的出现次数
        java.util.Map<Integer, Integer> lengthCounts = new java.util.HashMap<>();
        for (SegmentInfo segment : segments) {
            String pathPattern = extractPathPattern(segment.url);
            int len = pathPattern.length();
            lengthCounts.put(len, lengthCounts.getOrDefault(len, 0) + 1);
        }
        
        // 找出出现次数最多的前缀长度作为主长度
        int mainLength = 0;
        int mainLengthCount = 0;
        for (java.util.Map.Entry<Integer, Integer> entry : lengthCounts.entrySet()) {
            if (entry.getValue() > mainLengthCount) {
                mainLength = entry.getKey();
                mainLengthCount = entry.getValue();
            }
        }
        
        Log.d(TAG, "Prefix length counts: " + lengthCounts.toString());
        Log.d(TAG, "Main prefix length: " + mainLength + " (count=" + mainLengthCount + ")");
        
        // 标记前缀长度不同于主长度的片段为广告
        if (lengthCounts.size() > 1 && mainLengthCount > segments.size() * 0.3) {
            for (SegmentInfo segment : segments) {
                String pathPattern = extractPathPattern(segment.url);
                if (pathPattern.length() != mainLength) {
                    // 检查白名单
                    if (isInWhitelist(segment.url)) {
                        Log.d(TAG, "Segment in whitelist, skipping ad detection: " + segment.url);
                        continue;
                    }
                    segment.isAd = true;
                    adSegments.add(segment);
                    Log.d(TAG, "Ad detected by prefix length at position " + segment.index + 
                          ": " + segment.url + ", length=" + pathPattern.length());
                }
            }
        }
        
        // 方法2: 如果有多个路径模式，标记非主路径的片段为广告
        if (adSegments.isEmpty() && pathCounts.size() > 1 && mainPathCount > segments.size() * 0.1) {
            for (SegmentInfo segment : segments) {
                String pathPattern = extractPathPattern(segment.url);
                if (!pathPattern.equals(mainPathPattern)) {
                    // 检查白名单
                    if (isInWhitelist(segment.url)) {
                        Log.d(TAG, "Segment in whitelist, skipping ad detection: " + segment.url);
                        continue;
                    }
                    segment.isAd = true;
                    adSegments.add(segment);
                    Log.d(TAG, "Ad detected by path pattern at position " + segment.index + ": " + segment.url);
                }
            }
        }
        
        // 方法2: 检测DISCONTINUITY标记的广告片段组
        if (adSegments.isEmpty()) {
            // 找出所有DISCONTINUITY位置
            List<Integer> discontinuityPositions = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                if (segments.get(i).isDiscontinuity) {
                    discontinuityPositions.add(i);
                }
            }
            
            if (!discontinuityPositions.isEmpty()) {
                // 检测开头广告（第一个DISCONTINUITY之前的片段）
                int firstDiscontinuity = discontinuityPositions.get(0);
                if (firstDiscontinuity < segments.size() / 3) {
                    double totalDuration = 0;
                    for (int j = 0; j <= firstDiscontinuity; j++) {
                        totalDuration += segments.get(j).duration;
                    }
                    // 如果DISCONTINUITY前的总时长<60秒，可能是开头广告
                    if (totalDuration < 60 && totalDuration > 0) {
                        for (int j = 0; j <= firstDiscontinuity; j++) {
                            // 检查白名单
                            if (isInWhitelist(segments.get(j).url)) {
                                Log.d(TAG, "Segment in whitelist, skipping opening ad detection: " + segments.get(j).url);
                                continue;
                            }
                            segments.get(j).isAd = true;
                            adSegments.add(segments.get(j));
                            Log.d(TAG, "Opening ad detected at position " + j);
                        }
                    }
                }
                
                // 检测中间广告（两个DISCONTINUITY之间的片段）
                for (int d = 0; d < discontinuityPositions.size() - 1; d++) {
                    int startPos = discontinuityPositions.get(d) + 1;
                    int endPos = discontinuityPositions.get(d + 1);
                    
                    if (endPos > startPos) {
                        double totalDuration = 0;
                        for (int j = startPos; j <= endPos; j++) {
                            totalDuration += segments.get(j).duration;
                        }
                        // 如果中间片段组时长<60秒，可能是中间广告
                        if (totalDuration < 60 && totalDuration > 5) {
                            for (int j = startPos; j <= endPos; j++) {
                                // 检查白名单
                                if (isInWhitelist(segments.get(j).url)) {
                                    Log.d(TAG, "Segment in whitelist, skipping mid-roll ad detection: " + segments.get(j).url);
                                    continue;
                                }
                                segments.get(j).isAd = true;
                                adSegments.add(segments.get(j));
                                Log.d(TAG, "Mid-roll ad detected at position " + j);
                            }
                        }
                    }
                }
                
                // 检测结尾广告（最后一个DISCONTINUITY之后的片段）
                if (discontinuityPositions.size() >= 1) {
                    int lastDiscontinuity = discontinuityPositions.get(discontinuityPositions.size() - 1);
                    if (lastDiscontinuity > segments.size() * 2 / 3) {
                        double totalDuration = 0;
                        for (int j = lastDiscontinuity + 1; j < segments.size(); j++) {
                            totalDuration += segments.get(j).duration;
                        }
                        // 如果结尾片段组时长<60秒，可能是结尾广告
                        if (totalDuration < 60 && totalDuration > 0) {
                            for (int j = lastDiscontinuity + 1; j < segments.size(); j++) {
                                // 检查白名单
                                if (isInWhitelist(segments.get(j).url)) {
                                    Log.d(TAG, "Segment in whitelist, skipping post-roll ad detection: " + segments.get(j).url);
                                    continue;
                                }
                                segments.get(j).isAd = true;
                                adSegments.add(segments.get(j));
                                Log.d(TAG, "Post-roll ad detected at position " + j);
                            }
                        }
                    }
                }
            }
        }
        
        // 方法3: 检测文件名数字序列突变（广告片段的数字通常突变）
        if (adSegments.isEmpty()) {
            // 提取所有片段的数字序列
            List<Long> segmentNumbers = new ArrayList<>();
            for (SegmentInfo segment : segments) {
                long num = extractSegmentNumber(segment.url);
                segmentNumbers.add(num);
            }
            
            // 检测数字突变位置
            List<int[]> jumpRanges = new ArrayList<>();  // [start, end] 突变范围
            int jumpStart = -1;
            
            for (int i = 1; i < segmentNumbers.size(); i++) {
                long prev = segmentNumbers.get(i - 1);
                long curr = segmentNumbers.get(i);
                
                // 如果数字跳跃超过1000，认为是突变
                if (Math.abs(curr - prev) > 1000) {
                    if (jumpStart == -1) {
                        jumpStart = i;
                    }
                } else {
                    // 序列恢复正常，标记突变范围
                    if (jumpStart != -1) {
                        jumpRanges.add(new int[]{jumpStart, i - 1});
                        jumpStart = -1;
                    }
                }
            }
            
            // 处理末尾的突变
            if (jumpStart != -1) {
                jumpRanges.add(new int[]{jumpStart, segmentNumbers.size() - 1});
            }
            
            // 标记突变范围内的片段为广告
            for (int[] range : jumpRanges) {
                int start = range[0];
                int end = range[1];
                
                // 计算突变片段组的总时长
                double totalDuration = 0;
                for (int j = start; j <= end; j++) {
                    totalDuration += segments.get(j).duration;
                }
                
                // 如果突变片段组时长<120秒，很可能是广告
                if (totalDuration < 120 && totalDuration > 0) {
                    for (int j = start; j <= end; j++) {
                        // 检查白名单
                        if (isInWhitelist(segments.get(j).url)) {
                            Log.d(TAG, "Segment in whitelist, skipping number jump ad detection: " + segments.get(j).url);
                            continue;
                        }
                        segments.get(j).isAd = true;
                        adSegments.add(segments.get(j));
                        Log.d(TAG, "Number jump ad detected at position " + j + 
                              ", number=" + segmentNumbers.get(j) + 
                              ", duration=" + segments.get(j).duration);
                    }
                }
            }
        }
        
        // 方法4: 检测异常短片段组（广告通常时长较短且连续）
        if (adSegments.isEmpty()) {
            int consecutiveShortCount = 0;
            int shortStartIndex = -1;
            
            for (int i = 0; i < segments.size(); i++) {
                if (segments.get(i).duration < 1.0) {
                    if (shortStartIndex == -1) {
                        shortStartIndex = i;
                    }
                    consecutiveShortCount++;
                } else {
                    // 如果连续短片段数>=3且总时长<30秒，可能是广告
                    if (consecutiveShortCount >= 3) {
                        double totalDuration = 0;
                        for (int j = shortStartIndex; j < i; j++) {
                            totalDuration += segments.get(j).duration;
                        }
                        if (totalDuration < 30) {
                            for (int j = shortStartIndex; j < i; j++) {
                                // 检查白名单
                                if (isInWhitelist(segments.get(j).url)) {
                                    Log.d(TAG, "Segment in whitelist, skipping short segment ad detection: " + segments.get(j).url);
                                    continue;
                                }
                                segments.get(j).isAd = true;
                                adSegments.add(segments.get(j));
                                Log.d(TAG, "Short segment ad detected at position " + j);
                            }
                        }
                    }
                    consecutiveShortCount = 0;
                    shortStartIndex = -1;
                }
            }
        }
        
        return adSegments;
    }
    
    /**
     * 提取路径模式（用于识别不同来源的片段）
     */
    private String extractPathPattern(String url) {
        if (url == null) return "";
        
        try {
            // 如果是相对路径（只有文件名），提取文件名前缀
            // 例如: e57566bf8e4000073.ts -> e57566bf8e4000
            //       e57566bf8e40715435.ts -> e57566bf8e40 (不同前缀)
            if (!url.contains("/")) {
                int dotPos = url.lastIndexOf('.');
                String filename = dotPos > 0 ? url.substring(0, dotPos) : url;
                
                // 提取文件名的数字前缀部分（去掉末尾的数字序列）
                // e57566bf8e4000073 -> e57566bf8e4000
                int lastDigitStart = -1;
                for (int i = filename.length() - 1; i >= 0; i--) {
                    if (Character.isDigit(filename.charAt(i))) {
                        if (lastDigitStart == -1) {
                            lastDigitStart = i;
                        }
                    } else {
                        break;
                    }
                }
                
                if (lastDigitStart > 0) {
                    return filename.substring(0, lastDigitStart);
                }
                return filename;
            }
            
            // 有目录路径的情况，提取第一级目录
            int slashCount = 0;
            int secondSlash = -1;
            
            for (int i = 0; i < url.length(); i++) {
                if (url.charAt(i) == '/') {
                    slashCount++;
                    if (slashCount == 2) {
                        secondSlash = i;
                    } else if (slashCount == 3) {
                        return url.substring(secondSlash, i + 1);
                    }
                }
            }
            
            return url;
        } catch (Exception e) {
            return url;
        }
    }
    
    /**
     * 检测是否为Master Playlist并提取子播放列表URL
     * Master Playlist包含#EXT-X-STREAM-INF标签，指向子播放列表
     */
    private String extractSubM3U8Url(String content, String masterUrl) {
        if (content == null || content.isEmpty()) return null;
        
        try {
            String[] lines = content.split("\n");
            String subUrl = null;
            int maxBandwidth = 0;
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                
                // 检测#EXT-X-STREAM-INF标签（Master Playlist特征）
                if (line.startsWith("#EXT-X-STREAM-INF:")) {
                    // 提取BANDWIDTH
                    int bandwidth = 0;
                    String[] parts = line.split(",");
                    for (String part : parts) {
                        if (part.startsWith("BANDWIDTH=")) {
                            bandwidth = Integer.parseInt(part.substring(10));
                            break;
                        }
                    }
                    
                    // 获取下一行的子播放列表URL
                    if (i + 1 < lines.length) {
                        String nextLine = lines[i + 1].trim();
                        if (!nextLine.isEmpty() && !nextLine.startsWith("#")) {
                            // 选择最高带宽的流
                            if (bandwidth > maxBandwidth) {
                                maxBandwidth = bandwidth;
                                subUrl = nextLine;
                            }
                        }
                    }
                }
            }
            
            if (subUrl != null) {
                // 转换为绝对URL
                String baseUrlPath = extractBaseUrlPath(masterUrl);
                return toAbsoluteUrl(subUrl, baseUrlPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "extractSubM3U8Url error", e);
        }
        return null;
    }
    
    /**
     * 提取片段文件名中的数字序列
     * 例如: e57566bf8e4000073.ts -> 73, e57566bf8e40715435.ts -> 715435
     */
    private long extractSegmentNumber(String url) {
        if (url == null) return -1;
        
        try {
            // 提取文件名（不含扩展名）
            int lastSlash = url.lastIndexOf('/');
            String filename = url.substring(lastSlash + 1);
            int dotPos = filename.lastIndexOf('.');
            if (dotPos > 0) {
                filename = filename.substring(0, dotPos);
            }
            
            // 提取末尾的数字部分
            StringBuilder numStr = new StringBuilder();
            for (int i = filename.length() - 1; i >= 0; i--) {
                char c = filename.charAt(i);
                if (Character.isDigit(c)) {
                    numStr.insert(0, c);
                } else {
                    break;  // 遇到非数字字符停止
                }
            }
            
            if (numStr.length() > 0) {
                return Long.parseLong(numStr.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "extractSegmentNumber error: " + url, e);
        }
        return -1;
    }
    
    /**
     * 解析#EXTINF行获取时长
     */
    private double parseExtinfDuration(String line) {
        try {
            // #EXTINF:2.000000,
            int colon = line.indexOf(':');
            int comma = line.indexOf(',');
            if (colon > 0 && comma > colon) {
                return Double.parseDouble(line.substring(colon + 1, comma));
            }
        } catch (Exception e) {
            Log.e(TAG, "parseExtinfDuration error: " + line, e);
        }
        return 0;
    }
    
    /**
     * 提取m3u8的基础URL路径（用于转换相对路径）
     * 例如: https://example.com/path/to/index.m3u8 -> https://example.com/path/to/
     */
    private String extractBaseUrlPath(String m3u8Url) {
        if (m3u8Url == null) return "";
        
        int lastSlash = m3u8Url.lastIndexOf('/');
        if (lastSlash > 0) {
            return m3u8Url.substring(0, lastSlash + 1);
        }
        return m3u8Url;
    }
    
    /**
     * 将相对URL转换为绝对URL
     */
    private String toAbsoluteUrl(String segmentUrl, String baseUrlPath) {
        if (segmentUrl == null) return "";
        
        // 已经是绝对URL
        if (segmentUrl.startsWith("http://") || segmentUrl.startsWith("https://")) {
            return segmentUrl;
        }
        
        // 相对URL，拼接基础路径
        if (segmentUrl.startsWith("/")) {
            // 绝对路径，需要提取域名
            try {
                java.net.URL url = new java.net.URL(baseUrlPath);
                String domain = url.getProtocol() + "://" + url.getHost();
                if (url.getPort() != -1) {
                    domain += ":" + url.getPort();
                }
                return domain + segmentUrl;
            } catch (Exception e) {
                return baseUrlPath + segmentUrl.substring(1);
            }
        } else {
            // 相对路径
            return baseUrlPath + segmentUrl;
        }
    }
    
    /**
     * 生成缓存key
     */
    private String getCacheKey(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(url.hashCode());
        }
    }
    
    /**
     * 更新缓存索引
     */
    private void updateCacheIndex(String cacheKey, int adCount) {
        try {
            File indexFile = new File(mCacheDir, CACHE_INDEX_FILE);
            FileOutputStream fos = new FileOutputStream(indexFile, true);
            String entry = cacheKey + "|" + adCount + "\n";
            fos.write(entry.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "updateCacheIndex error", e);
        }
    }
    
    /**
     * 从缓存索引获取广告片段数
     */
    private int getAdCountFromCacheIndex(String cacheKey) {
        try {
            File indexFile = new File(mCacheDir, CACHE_INDEX_FILE);
            if (!indexFile.exists()) return 0;
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(indexFile), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2 && parts[0].equals(cacheKey)) {
                    reader.close();
                    return Integer.parseInt(parts[1]);
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "getAdCountFromCacheIndex error", e);
        }
        return 0;
    }
    
    /**
     * 清理旧缓存（保留最近100个文件）
     */
    private void cleanOldCache() {
        File[] files = mCacheDir.listFiles();
        if (files == null || files.length <= 100) return;
        
        // 按修改时间排序，删除最旧的
        java.util.Arrays.sort(files, (a, b) -> 
            Long.compare(a.lastModified(), b.lastModified()));
        
        int toDelete = files.length - 100;
        for (int i = 0; i < toDelete; i++) {
            if (!files[i].getName().equals(CACHE_INDEX_FILE)) {
                files[i].delete();
            }
        }
        Log.d(TAG, "Cleaned " + toDelete + " old cache files");
    }
    
    /**
     * 清除所有缓存
     */
    public void clearCache() {
        File[] files = mCacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        Log.d(TAG, "Cache cleared");
    }
    
    /**
     * 清除特定URL的缓存
     * @param originalUrl 原始m3u8 URL
     */
    public void clearCacheForUrl(String originalUrl) {
        if (originalUrl == null) return;
        
        String cacheKey = getCacheKey(originalUrl);
        File cachedFile = new File(mCacheDir, cacheKey + ".m3u8");
        if (cachedFile.exists()) {
            boolean deleted = cachedFile.delete();
            Log.d(TAG, "Cleared cache for URL: " + originalUrl + ", deleted=" + deleted);
        }
        
        // 从索引文件中移除
        removeFromCacheIndex(cacheKey);
    }
    
    /**
     * 从缓存索引中移除
     */
    private void removeFromCacheIndex(String cacheKey) {
        try {
            File indexFile = new File(mCacheDir, CACHE_INDEX_FILE);
            if (!indexFile.exists()) return;
            
            // 读取所有行
            java.util.List<String> lines = new java.util.ArrayList<>();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(indexFile), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2 && !parts[0].equals(cacheKey)) {
                    lines.add(line);
                }
            }
            reader.close();
            
            // 重写文件
            PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(indexFile), "UTF-8"));
            for (String l : lines) {
                writer.println(l);
            }
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "removeFromCacheIndex error", e);
        }
    }
    
    /**
     * 检查URL是否是HTTP的m3u8
     */
    public static boolean isHttpM3U8(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return (lower.startsWith("http://") || lower.startsWith("https://")) 
            && lower.contains(".m3u8");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        mExecutor.shutdown();
    }
    
    // 内部类：片段信息
    private static class SegmentInfo {
        int index;
        double duration;
        String url;
        int lineNumber;
        boolean isDiscontinuity;  // 此片段后是否有DISCONTINUITY
        boolean isAd;             // 是否是广告片段
        boolean needsDiscontinuity; // 输出时是否需要DISCONTINUITY标记
    }
    
    // 内部类：解析结果
    private static class M3U8ParseResult {
        String cleanedContent;
        int adSegmentsRemoved;
    }
}
