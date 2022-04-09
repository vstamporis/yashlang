package su.sadrobot.yashlang.controller;

import org.schabi.newpipe.DownloaderTestImpl;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import su.sadrobot.yashlang.ConfigOptions;

public class StreamHelperTest {
    private static boolean assertEqual(final String actual, final String expected) {
        //System.out.println(message + ": ");
        final boolean pass = actual.equals(expected);
        if (pass) {
            System.out.print("PASS: ");
        } else {
            System.out.print("FAIL: ");
        }
        System.out.println("test if actual value '" + actual + "' is EQUAL to expected '" + expected + "'");

        return pass;
    }

    private static boolean assertNull(final Object actual) {
        //System.out.println(message + ": ");
        final boolean pass = actual == null;
        if (pass) {
            System.out.print("PASS: ");
        } else {
            System.out.print("FAIL: ");
        }
        System.out.println("test if actual value is NULL");

        return pass;
    }

    public static void main(String args[]) {
        // https://ru.wikipedia.org/wiki/480p
        // разрешение может быть указано как 480p или 480p60 (60 - количество кадров в секунду)
        System.out.println("480p60".replaceAll("p.*", "") );

        // вывести список потоков для ролика (не тест, требуется интернет)
        showSampleStreams();
        showSampleStreams2();

        testNextPlaybackStreamMaxRes();
        testNextPlaybackStreamMinRes();
        testNextPlaybackStreamForRes();
    }

    public static void showSampleStreams() {
        System.out.println("***** TEST: showSampleStreams *****");
        //printStreamsInfo();
        // Лунтик: есть несколько потоков с одинаковым форматом и одинаковым разрешением
        /// https://www.youtube.com/watch?v=4BkgG03EG7U
        final String itemUrl = "https://www.youtube.com/watch?v=4BkgG03EG7U";
        //final String itemUrl = "https://www.youtube.com/watch?v=5t4YzhKeI6A";
        //final String itemUrl = "https://mult.sadrobot.su/w/6bnGpropidecqT8spxbB9i";
        //final String itemUrl = "https://mult.sadrobot.su/videos/watch/6bnGpropidecqT8spxbB9i";

        try {
            // библиотека nanojson в build.gradle должна быть добавлена вот так:
            // testImplementation "com.github.TeamNewPipe:nanojson"
            // как в https://github.com/TeamNewPipe/NewPipeExtractor/blob/dev/extractor/build.gradle
            // иначе будет NullPointer в процессе экстракта потоков
            final StreamHelper.StreamSources streamSources = ContentLoader.getInstance().extractStreams(itemUrl);
            for (final StreamHelper.StreamInfo stream : streamSources.getVideoStreams()) {
                System.out.println(stream.getResolution() + " " +
                        stream.getFormatName() + " " +
                        stream.getStreamType());
            }
            for (final StreamHelper.StreamInfo stream : streamSources.getAudioStreams()) {
                System.out.println(stream.getResolution() + " " +
                        stream.getFormatName() + " " +
                        stream.getStreamType());
            }

            // для https://www.youtube.com/watch?v=4BkgG03EG7U
            // получаем:
            // 1080p WebM VIDEO
            // 1080p MPEG-4 VIDEO
            // 720p WebM VIDEO
            // 720p MPEG-4 VIDEO
            // 720p MPEG-4 BOTH
            // 480p WebM VIDEO
            // 480p MPEG-4 VIDEO
            // 360p WebM VIDEO
            // 360p MPEG-4 VIDEO
            // 360p MPEG-4 BOTH
            // 240p WebM VIDEO
            // 240p MPEG-4 VIDEO
            // 144p WebM VIDEO
            // 144p MPEG-4 VIDEO
            // 144p 3GPP BOTH
            // null m4a AUDIO
            // null m4a AUDIO
            // null WebM Opus AUDIO
            // null WebM Opus AUDIO
            // null WebM Opus AUDIO
        } catch (ExtractionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void showSampleStreams2() {
        System.out.println("***** TEST: showSampleStreams2 *****");
        final String itemUrl = "https://www.youtube.com/watch?v=4BkgG03EG7U";
        //final String itemUrl = "https://mult.sadrobot.su/videos/watch/6bnGpropidecqT8spxbB9i";
        try {
            NewPipe.init(DownloaderTestImpl.getInstance());
            final StreamExtractor extractor = ContentLoader.getInstance().getStreamExtractor(itemUrl);
            extractor.fetchPage();

            for (final VideoStream stream : extractor.getVideoStreams()) {
                System.out.println(stream.getResolution() + " " +
                        stream.getFormat().getName() + " " +
                        stream.getFormat().getMimeType() + " " +
                        stream.getFormat().getSuffix());
            }
            System.out.println();
            for (final VideoStream stream : extractor.getVideoOnlyStreams()) {
                System.out.println(stream.getResolution() + " " +
                        stream.getFormat().getName() + " " +
                        stream.getFormat().getMimeType() + " " +
                        stream.getFormat().getSuffix());
            }
            System.out.println();
            for (final AudioStream stream : extractor.getAudioStreams()) {
                System.out.println(stream.getBitrate() + " " +
                        stream.getFormat().getName() + " " +
                        stream.getFormat().getMimeType() + " " +
                        stream.getFormat().getSuffix());
            }

            // для: https://www.youtube.com/watch?v=4BkgG03EG7U
            // 144p 3GPP video/3gpp 3gp
            // 360p MPEG-4 video/mp4 mp4
            // 720p MPEG-4 video/mp4 mp4
            // 1080p MPEG-4 video/mp4 mp4
            // 1080p WebM video/webm webm
            //
            // 720p MPEG-4 video/mp4 mp4
            // 720p WebM video/webm webm
            // 480p MPEG-4 video/mp4 mp4
            // 480p WebM video/webm webm
            // 360p MPEG-4 video/mp4 mp4
            // 360p WebM video/webm webm
            // 240p MPEG-4 video/mp4 mp4
            // 240p WebM video/webm webm
            // 144p MPEG-4 video/mp4 mp4
            // 144p WebM video/webm webm
            //
            // 50001 m4a audio/mp4 m4a
            // 130461 m4a audio/mp4 m4a
            // 62153 WebM Opus audio/webm webm
            // 81639 WebM Opus audio/webm webm
            // 155264 WebM Opus audio/webm webm

            // для https://mult.sadrobot.su/videos/watch/6bnGpropidecqT8spxbB9i
            // 720p MPEG-4 video/mp4 mp4
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testNextPlaybackStreamMaxRes() {
        System.out.println("***** TEST: testNextPlaybackStreamMaxRes *****");

        final List<StreamHelper.StreamInfo> videoSources1 = new ArrayList<>();
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "1080p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "480p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "360p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "240p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "144p")));

        StreamHelper.StreamInfo nextStream;
        nextStream = StreamHelper.getNextPlaybackStreamMaxRes(videoSources1, null);
        assertEqual(nextStream.getResolution(), "1080p");

        nextStream = StreamHelper.getNextPlaybackStreamMaxRes(videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "480p");

        nextStream = StreamHelper.getNextPlaybackStreamMaxRes(videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "360p");

        nextStream = StreamHelper.getNextPlaybackStreamMaxRes(videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "240p");

        nextStream = StreamHelper.getNextPlaybackStreamMaxRes(videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "144p");

        nextStream = StreamHelper.getNextPlaybackStreamMaxRes(videoSources1, nextStream);
        assertNull(nextStream);
    }

    public static void testNextPlaybackStreamMinRes() {
        System.out.println("***** TEST: testNextPlaybackStreamMinRes *****");

        final List<StreamHelper.StreamInfo> videoSources1 = new ArrayList<>();
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "1080p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "480p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "360p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "240p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "144p")));

        StreamHelper.StreamInfo nextStream;
        nextStream = StreamHelper.getNextPlaybackStreamMinRes(videoSources1, null);
        assertEqual(nextStream.getResolution(), "144p");

        nextStream = StreamHelper.getNextPlaybackStreamMinRes(videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "240p");

        nextStream = StreamHelper.getNextPlaybackStreamMinRes(videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "360p");

        nextStream = StreamHelper.getNextPlaybackStreamMinRes(videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "480p");

        nextStream = StreamHelper.getNextPlaybackStreamMinRes(videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "1080p");

        nextStream = StreamHelper.getNextPlaybackStreamMinRes(videoSources1, nextStream);
        assertNull(nextStream);
    }

    public static void testNextPlaybackStreamForRes() {
        System.out.println("***** TEST: testNextPlaybackStreamForRes *****");

        // video sources list - v1
        final List<StreamHelper.StreamInfo> videoSources1 = new ArrayList<>();
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "1080p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "480p")));
        // пусть этого варианта нет в списке
        //videoSources1.add(new VideoStream("urll", MediaFormat.MPEG_4, "360p"));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "240p")));
        videoSources1.add(new StreamHelper.StreamInfo(new VideoStream("urll", MediaFormat.MPEG_4, "144p")));

        StreamHelper.StreamInfo nextStream;

        // ***** TEST-0 *****
        // выбор первого элемента
        // проверка точного совпадения
        nextStream = StreamHelper.getNextPlaybackStreamForRes("480p",
                ConfigOptions.VideoStreamSelectPreferRes.HIGHER_RES, videoSources1, null);
        assertEqual(nextStream.getResolution(), "480p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("1080p",
                ConfigOptions.VideoStreamSelectPreferRes.HIGHER_RES, videoSources1, null);
        assertEqual(nextStream.getResolution(), "1080p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("1081p",
                ConfigOptions.VideoStreamSelectPreferRes.HIGHER_RES, videoSources1, null);
        assertEqual(nextStream.getResolution(), "1080p");


        nextStream = StreamHelper.getNextPlaybackStreamForRes("480p",
                ConfigOptions.VideoStreamSelectPreferRes.LOWER_RES, videoSources1, null);
        assertEqual(nextStream.getResolution(), "480p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("144p",
                ConfigOptions.VideoStreamSelectPreferRes.LOWER_RES, videoSources1, null);
        assertEqual(nextStream.getResolution(), "144p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("140p",
                ConfigOptions.VideoStreamSelectPreferRes.LOWER_RES, videoSources1, null);
        assertEqual(nextStream.getResolution(), "144p");

        // ***** TEST-1 *****
        // Последовательный выбор элементов
        // предпочитать видео с более высоким разрешением, следующее за текущим
        // здесь в настройках указано разрешение, которого нет в списке
        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.HIGHER_RES, videoSources1, null);
        assertEqual(nextStream.getResolution(), "480p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.HIGHER_RES, videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "1080p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.HIGHER_RES, videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "240p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.HIGHER_RES, videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "144p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.HIGHER_RES, videoSources1, nextStream);
        assertNull(nextStream);

        // ***** TEST-2 *****
        // Последовательный выбор элементов:
        // предпочитать видео с более низким разрешением, следующее за текущим
        // здесь в настройках указано разрешение, которого нет в списке
        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.LOWER_RES, videoSources1, null);
        assertEqual(nextStream.getResolution(), "240p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.LOWER_RES, videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "144p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.LOWER_RES, videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "480p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.LOWER_RES, videoSources1, nextStream);
        assertEqual(nextStream.getResolution(), "1080p");

        nextStream = StreamHelper.getNextPlaybackStreamForRes("360p",
                ConfigOptions.VideoStreamSelectPreferRes.LOWER_RES, videoSources1, nextStream);
        assertNull(nextStream);
    }
}
