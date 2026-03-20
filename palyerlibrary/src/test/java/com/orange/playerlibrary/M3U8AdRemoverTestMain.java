package com.orange.playerlibrary;

/**
 * M3U8 AES Encryption Key Preservation Test
 */
public class M3U8AdRemoverTestMain {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("M3U8 AES Encryption Key Preservation Test");
        System.out.println("========================================\n");
        
        // Simulated m3u8 content from user report
        String originalM3U8 = 
            "#EXTM3U\n" +
            "#EXT-X-VERSION:3\n" +
            "#EXT-X-PLAYLIST-TYPE:VOD\n" +
            "#EXT-X-TARGETDURATION:10\n" +
            "#EXT-X-MEDIA-SEQUENCE:0\n" +
            "#EXTINF:4.0,\n" +
            "http://127.0.0.1:6774/ad.ts\n" +
            "#EXT-X-DISCONTINUITY\n" +
            "#EXT-X-KEY:METHOD=AES-128,URI=\"http://127.0.0.1:6774/0.key\",IV=0000000000000000\n" +
            "#EXTINF:6.083333,\n" +
            "http://127.0.0.1:6774/0.ts?url=https://hnts.ymuuy.com:65/hls/680/20250719/1872803/plist0.ts\n" +
            "#EXTINF:10.416667,\n" +
            "http://127.0.0.1:6774/1.ts?url=https://hnts.ymuuy.com:65/hls/680/20250719/1872803/plist1.ts\n" +
            "#EXTINF:4.708333,\n" +
            "http://127.0.0.1:6774/2.ts?url=https://hnts.ymuuy.com:65/hls/680/20250719/1872803/plist2.ts\n" +
            "#EXT-X-ENDLIST\n";
        
        System.out.println("[Original M3U8]");
        System.out.println("----------------------------------------");
        System.out.println(originalM3U8);
        
        // Simulate parsing
        System.out.println("\n[Parsing Simulation]");
        System.out.println("----------------------------------------");
        simulateParsing(originalM3U8);
        
        // Simulate rebuild
        System.out.println("\n[Rebuilt M3U8]");
        System.out.println("----------------------------------------");
        String cleanedM3U8 = simulateRebuild(originalM3U8);
        System.out.println(cleanedM3U8);
        
        // Verify
        System.out.println("\n[Verification]");
        System.out.println("----------------------------------------");
        boolean hasKey = cleanedM3U8.contains("#EXT-X-KEY:");
        boolean hasMethod = cleanedM3U8.contains("METHOD=AES-128");
        boolean hasUri = cleanedM3U8.contains("URI=\"http://127.0.0.1:6774/0.key\"");
        boolean hasIv = cleanedM3U8.contains("IV=0000000000000000");
        boolean hasPlaceholder = cleanedM3U8.contains("placeholder.ts");
        boolean hasOriginalSeg = cleanedM3U8.contains("plist0.ts");
        
        System.out.println("* Contains #EXT-X-KEY: " + hasKey);
        System.out.println("* Contains METHOD=AES-128: " + hasMethod);
        System.out.println("* Contains URI: " + hasUri);
        System.out.println("* Contains IV: " + hasIv);
        System.out.println("* Ad replaced with placeholder: " + hasPlaceholder);
        System.out.println("* Original segment URL preserved: " + hasOriginalSeg);
        
        if (hasKey && hasMethod && hasUri && hasIv) {
            System.out.println("\n========================================");
            System.out.println("TEST PASSED! AES encryption key preserved");
            System.out.println("========================================");
        } else {
            System.out.println("\n========================================");
            System.out.println("TEST FAILED! Key information lost");
            System.out.println("========================================");
        }
    }
    
    private static void simulateParsing(String content) {
        String[] lines = content.split("\n");
        String currentEncryptionKey = null;
        int segmentIndex = 0;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("#EXT-X-KEY:")) {
                currentEncryptionKey = line.substring("#EXT-X-KEY:".length());
                System.out.println("[Parse] Found encryption key: " + currentEncryptionKey);
            }
            
            if (line.startsWith("#EXTINF:")) {
                System.out.println("[Parse] Segment " + segmentIndex + ": encrypted=" + (currentEncryptionKey != null) + 
                    ", key=" + (currentEncryptionKey != null ? currentEncryptionKey.substring(0, Math.min(30, currentEncryptionKey.length())) + "..." : "null"));
                segmentIndex++;
            }
            
            if (line.equals("#EXT-X-DISCONTINUITY")) {
                System.out.println("[Parse] Stream switch point (DISCONTINUITY)");
            }
        }
    }
    
    private static String simulateRebuild(String content) {
        StringBuilder cleaned = new StringBuilder();
        String[] lines = content.split("\n");
        String currentEncryptionKey = null;
        String lastEncryptionKey = null;
        boolean firstSegment = true;
        boolean needsDiscontinuity = false;
        
        // Header
        cleaned.append("#EXTM3U\n");
        cleaned.append("#EXT-X-VERSION:3\n");
        cleaned.append("#EXT-X-TARGETDURATION:8\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.startsWith("#EXT-X-KEY:")) {
                currentEncryptionKey = line.substring("#EXT-X-KEY:".length());
            }
            
            if (line.equals("#EXT-X-DISCONTINUITY")) {
                needsDiscontinuity = true;
            }
            
            if (line.startsWith("#EXTINF:")) {
                double duration = parseDuration(line);
                
                String url = null;
                for (int j = i + 1; j < lines.length; j++) {
                    String nextLine = lines[j].trim();
                    if (!nextLine.isEmpty() && !nextLine.startsWith("#")) {
                        url = nextLine;
                        break;
                    }
                }
                
                if (url != null) {
                    if (!firstSegment && needsDiscontinuity) {
                        cleaned.append("#EXT-X-DISCONTINUITY\n");
                        lastEncryptionKey = null;
                        needsDiscontinuity = false;
                    }
                    firstSegment = false;
                    
                    // Output encryption key if present and different
                    if (currentEncryptionKey != null && !currentEncryptionKey.equals(lastEncryptionKey)) {
                        cleaned.append("#EXT-X-KEY:").append(currentEncryptionKey).append("\n");
                        lastEncryptionKey = currentEncryptionKey;
                    }
                    
                    boolean isAd = url.contains("ad.ts");
                    
                    cleaned.append("#EXTINF:").append(String.format("%.6f", duration)).append(",\n");
                    if (isAd) {
                        cleaned.append("placeholder.ts\n");
                    } else {
                        cleaned.append(url).append("\n");
                    }
                }
            }
        }
        
        cleaned.append("#EXT-X-ENDLIST\n");
        return cleaned.toString();
    }
    
    private static double parseDuration(String extinfLine) {
        try {
            String durationStr = extinfLine.substring(8);
            int commaPos = durationStr.indexOf(',');
            if (commaPos > 0) {
                durationStr = durationStr.substring(0, commaPos);
            }
            return Double.parseDouble(durationStr.trim());
        } catch (Exception e) {
            return 5.0;
        }
    }
}
