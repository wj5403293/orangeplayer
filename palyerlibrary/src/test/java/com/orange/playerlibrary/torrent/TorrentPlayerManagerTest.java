package com.orange.playerlibrary.torrent;

import org.junit.Test;
import org.junit.Before;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Unit tests for TorrentPlayerManager
 * Note: Some tests require jlibtorrent native library which may not be available in unit test environment
 */
public class TorrentPlayerManagerTest {

    @Before
    public void setUp() {
        // Initialize before each test
    }

    @Test
    public void testIsTorrentUrl_magnetLink() {
        String magnetUrl = "magnet:?xt=urn:btih:08ada5a7a6183aae1e09d831df6748d566095a10&dn=Sintel";
        assertTrue("Magnet link should be detected as torrent URL", TorrentSupport.isTorrentUrl(magnetUrl));
    }

    @Test
    public void testIsTorrentUrl_torrentFile() {
        String torrentUrl = "http://example.com/file.torrent";
        assertTrue(".torrent URL should be detected as torrent URL", TorrentSupport.isTorrentUrl(torrentUrl));
    }

    @Test
    public void testIsTorrentUrl_regularUrl() {
        String regularUrl = "http://example.com/video.mp4";
        assertFalse("Regular URL should not be detected as torrent URL", TorrentSupport.isTorrentUrl(regularUrl));
    }

    @Test
    public void testIsTorrentUrl_magnetUppercase() {
        String magnetUrl = "MAGNET:?xt=urn:btih:abc123";
        assertTrue("Uppercase MAGNET should be detected", TorrentSupport.isTorrentUrl(magnetUrl));
    }

    @Test
    public void testIsTorrentUrl_torrentProtocol() {
        String torrentUrl = "torrent:http://example.com/file.torrent";
        assertTrue("torrent: protocol should be detected", TorrentSupport.isTorrentUrl(torrentUrl));
    }

    @Test
    public void testIsTorrentUrl_torrentQueryParam() {
        String torrentUrl = "http://example.com/file.torrent?info_hash=abc123";
        assertTrue(".torrent with query params should be detected", TorrentSupport.isTorrentUrl(torrentUrl));
    }
}
