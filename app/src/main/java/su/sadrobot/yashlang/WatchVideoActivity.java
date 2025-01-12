package su.sadrobot.yashlang;

/*
 * Created by Anton Moiseev (sadr0b0t) in 2019.
 *
 * Copyright (C) Anton Moiseev 2019 <github.com/sadr0b0t>
 * WatchVideoActivity.java is part of YaShlang.
 *
 * YaShlang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * YaShlang is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with YaShlang.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import su.sadrobot.yashlang.controller.ContentLoader;
import su.sadrobot.yashlang.controller.StreamHelper;
import su.sadrobot.yashlang.controller.VideoItemActions;
import su.sadrobot.yashlang.controller.VideoThumbManager;
import su.sadrobot.yashlang.model.PlaylistInfo;
import su.sadrobot.yashlang.model.StreamCache;
import su.sadrobot.yashlang.model.VideoDatabase;
import su.sadrobot.yashlang.model.VideoItem;
import su.sadrobot.yashlang.view.OnListItemClickListener;
import su.sadrobot.yashlang.view.VideoItemArrayAdapter;
import su.sadrobot.yashlang.view.VideoItemMultPlaylistsOnlyNewOnlineDataSourceFactory;
import su.sadrobot.yashlang.view.VideoItemOnlineDataSourceFactory;
import su.sadrobot.yashlang.view.VideoItemOnlyNewOnlineDataSourceFactory;
import su.sadrobot.yashlang.view.VideoItemPagedListAdapter;


public class WatchVideoActivity extends AppCompatActivity {
    // https://github.com/google/ExoPlayer
    // https://exoplayer.dev/
    // https://exoplayer.dev/hello-world.html
    // https://exoplayer.dev/ui-components.html
    // https://androidwave.com/play-youtube-video-in-exoplayer/

    /**
     * Загрузить информацию о видео по ID из базы
     */
    public static final String PARAM_VIDEO_ID = "PARAM_VIDEO_ID";

    /**
     * Загрузить информацию о видео онлайн
     */
    public static final String PARAM_VIDEO_ITEM_URL = "PARAM_VIDEO_ITEM_URL";


    /**
     * Режим для списка рекомендаций: значение из RecommendationsMode
     */
    public static final String PARAM_RECOMMENDATIONS_MODE = "PARAM_RECOMMENDATIONS_MODE";

    /**
     * Строка поиска для списка рекомендаций в режиме "результат поиска по запросу" или
     * фильтр плейлиста в режиме "плейлист по идентификатору"
     * (PARAM_RECOMMENDATIONS_MODE=RecommendationsMode.SEARCH_STR или RecommendationsMode.PLAYLIST_ID).
     * Список рекомендаций - все видео, найденные в базе по поисковой строке.
     *
     */
    public static final String PARAM_SEARCH_STR = "PARAM_SEARCH_STR";

    /**
     * Айди плейлиста для списка рекомендаций в режиме "плейлист по идентификатору"
     * (PARAM_RECOMMENDATIONS_MODE=RecommendationsMode.PLAYLIST_ID или RecommendationsMode.PLAYLIST_NEW).
     * Список рекомендаций - плейлист.
     */
    public static final String PARAM_PLAYLIST_ID = "PARAM_PLAYLIST_ID";

    /**
     * Адрес плейлиста для списка рекомендаций в режиме "плейлист онлайн по адресу"
     * (PARAM_RECOMMENDATIONS_MODE=RecommendationsMode.PLAYLIST_URL).
     * Список рекомендаций - плейлист, загруженный онлайн.
     */
    public static final String PARAM_PLAYLIST_URL = "PARAM_PLAYLIST_URL";

    /**
     * Показыывать в рекомендациях все видео, а не только разрешенные (enabled): true/false
     * (в режиме PLAYLIST_ID)
     * По умолчанию: false
     */
    public static final String PARAM_SHOW_ALL = "PARAM_SHOW_ALL";

    /**
     * Перемешать рекоменации: true/false
     * (в режиме PLAYLIST_ID)
     * По умолчанию: false
     */
    public static final String PARAM_SHUFFLE = "PARAM_SHUFFLE";

    /**
     * Сортировать рекомендации: ConfigOptions.SortBy: TIME_ADDED, NAME, DURATION
     * (в режиме PLAYLIST_ID)
     * По умолчанию: false
     */
    public static final String PARAM_SORT_BY = "PARAM_SORT_BY";

    /**
     * Сортировать рекомендации по возрастанию или по убыванию: true/false
     * (в режиме PLAYLIST_ID)
     * По умолчанию: false (сортировать по убыванию)
     */
    public static final String PARAM_SORT_DIR_ASCENDING = "PARAM_SORT_DIR_ASCENDING";

    /**
     * Режимы для списка рекомендаций
     */
    public enum RecommendationsMode {
        OFF, RANDOM, PLAYLIST_ID, PLAYLIST_URL, PLAYLIST_NEW, ALL_NEW, SEARCH_STR, STARRED
    }

    private enum PlayerState {
        EMPTY, LOADING, ERROR, LOADED, NOTHING_TO_PLAY
    }

    private enum PlayerMode {
        VIDEO, AUDIO
    }

    private PlayerView videoPlayerView;
    private View audioPlayerView;
    private ImageView audioPlayerThumbImg;
    private PlayerControlView videoPlayerControlView;
    private TextView streamInfoTxt;
    private ImageButton prevVideoBtn;
    private ImageButton nextVideoBtn;

    private View videoPlayerErrorView;
    private TextView videoLoadErrorTxt;
    private Button reloadOnErrorBtn;

    private View videoPlayerNothingToPlayView;
    private Button selectStreamBtn;

    private View videoPlayerLoadingView;

    private RecyclerView videoList;

    private Toolbar toolbar;
    private CheckBox starredCheck;

    private com.google.android.exoplayer2.upstream.DataSource.Factory videoDataSourceFactory;

    private VideoItem currentVideo;

    // для функции перехода на следующее видео
    private int currentVideoPosition = -1;
    private final Map<Long, Integer> posMap = new HashMap<>();
    private final Stack<VideoItem> playbackHistory = new Stack<>();

    private boolean stateFullscreen = false;

    private PlayerState playerState = PlayerState.EMPTY;
    private PlayerMode playerMode = PlayerMode.VIDEO;
    private String videoLoadErrorMsg = "";

    // рекомендации
    private LiveData<PagedList<VideoItem>> videoItemsLiveData;

    private final Handler handler = new Handler();

    // будем загружать видео в фоне, но строго последовательно
    //private ExecutorService videoLoadingExecutor = Executors.newFixedThreadPool(1);
    //private ExecutorService videoLoadingExecutor = Executors.newSingleThreadExecutor();
    // Вообще, если мы начали загружать новое видео, а потом поставили еще одно видео в очередь на загрузку,
    // а потом после него еще одно, то загружать второе видео вообще не обязательно - загружать
    // нужно только самое последнее видео после того, как завершится загрузка первого видео,
    // раз уж она началась (по-хорошему, ее тоже можно прервать, но это отдельная история).
    // По этой причине у нас ThreadPool будет содержать в очереди на выполнение всегда только один элемент:
    // один поток выполняется (должен быть уже извлечен из очереди), еще одно задание ожидает,
    // если добавляется новое задание, то оно заменяет ожидающее.
    // Как ни странно, в стандартных реализациях BlockingQueue такого варианта поведения не нашлось,
    // поэтому придется переопределить добавление элементов в очередь самостоятельно.
    // по мотивам:
    // https://www.javamex.com/tutorials/threads/thread_pools_queues.shtml
    // https://askdev.ru/q/ogranichennaya-po-razmeru-ochered-soderzhaschaya-poslednie-n-elementov-v-java-10569/
    // (похоже на автоматический перевод с английского, но там нет ссылки на источник)
    // здесь:
    //   - конструктор ThreadPoolExecutor с LinkedBlockingQueue взял из Executors.newSingleThreadExecutor
    //   - ThreadPoolExecutor.execute вызывает queue.offer, а не queue.add, поэтому переопределяем его
    private final ExecutorService videoLoaderExecutor =
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>() {
                        @Override
                        public boolean offer(Runnable o) {
                            super.clear();
                            return super.offer(o);
                        }
                    });

    // пул потоков для загрузки иконок видео, логика аналогичная videoLoadingExecutor
    private final ExecutorService videoThumbLoaderExecutor =
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>() {
                        @Override
                        public boolean offer(Runnable o) {
                            super.clear();
                            return super.offer(o);
                        }
                    });

    private final RecyclerView.AdapterDataObserver emptyListObserver = new RecyclerView.AdapterDataObserver() {
        // https://stackoverflow.com/questions/47417645/empty-view-on-a-recyclerview
        // https://stackoverflow.com/questions/27414173/equivalent-of-listview-setemptyview-in-recyclerview
        // https://gist.github.com/sheharyarn/5602930ad84fa64c30a29ab18eb69c6e
        private void checkIfEmpty() {
            // видимость некоторых элементов управления зависит от наличия элементов в
            // списке рекомендаций, а они могут загружаться в фоне
            updateControlsVisibility();
        }

        @Override
        public void onChanged() {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkIfEmpty();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_watch_video);

        videoPlayerView = findViewById(R.id.video_player_view);
        audioPlayerView = findViewById(R.id.audio_player_view);
        audioPlayerThumbImg = findViewById(R.id.audio_player_thumb_img);
        videoPlayerControlView = findViewById(R.id.video_player_control_view);
        streamInfoTxt = findViewById(R.id.stream_info_txt);
        prevVideoBtn = findViewById(R.id.prev_video_btn);
        nextVideoBtn = findViewById(R.id.next_video_btn);

        videoPlayerErrorView = findViewById(R.id.video_player_error_view);
        videoLoadErrorTxt = findViewById(R.id.video_load_error_txt);
        reloadOnErrorBtn = findViewById(R.id.reload_btn);

        videoPlayerNothingToPlayView = findViewById(R.id.video_player_nothing_to_play_view);
        selectStreamBtn = findViewById(R.id.select_stream_btn);


        videoPlayerLoadingView = findViewById(R.id.video_player_loading_view);

        videoList = findViewById(R.id.video_recommend_list);

        toolbar = findViewById(R.id.toolbar);

        // https://developer.android.com/training/appbar
        // https://www.vogella.com/tutorials/AndroidActionBar/article.html#custom-views-in-the-action-bar
        setSupportActionBar(toolbar);
        // кнопка "Назад" на акшенбаре
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                // Прячем панель навигации, т.к. при выборе меню на акшенбаре она появляется опять.
                hideNavigationBar();
            }
        });

        prevVideoBtn.setEnabled(false);
        prevVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playbackHistory.size() > 1) {
                    playbackHistory.pop();
                    playVideoItem(playbackHistory.pop(), false);

                    updateControlsVisibility();
                }
            }
        });

        nextVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoList.getAdapter() == null) {
                    // по-хорошему, мы не должны сюда попасть, т.к. в этом случае кнопка будет скрыта
                    return;
                }

                // переходим на следующее видео по списку рекомендаций
                // если мы на последней рекомендации, начинаем с начала
                final int nextVideoPosition = currentVideoPosition >= videoList.getAdapter().getItemCount() - 1 ?
                        0 : currentVideoPosition + 1;
                final VideoItem item;
                if (videoList.getAdapter().getItemCount() > 0) {
                    if (videoList.getAdapter() instanceof VideoItemPagedListAdapter) {
                        // здесь не случайные рекомендации, а, например, список выдачи по поисковому запросу
                        item = ((VideoItemPagedListAdapter) videoList.getAdapter()).getItem(nextVideoPosition);
                    } else if (videoList.getAdapter() instanceof VideoItemArrayAdapter) {
                        // здесь скорее всего случайные рекомендации
                        item = ((VideoItemArrayAdapter) videoList.getAdapter()).getItem(nextVideoPosition);
                    } else {
                        // сюда не попадём
                        item = null;
                    }
                } else {
                    item = null;
                }
                if (item != null) {
                    posMap.put(item.getId(), nextVideoPosition);
                    playVideoItem(item, false);
                }
            }
        });


        // Плеер
        final SimpleExoPlayer exoPlayer = new SimpleExoPlayer.Builder(this).build();
        videoDataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "yashlang"));

        videoPlayerView.requestFocus();
        videoPlayerView.setPlayer(exoPlayer);

        // контроллер отдельно, чтобы красиво добавить справа и слева от плеера кнопки назад и вперед
        videoPlayerView.setUseController(false);
        videoPlayerControlView.setPlayer(exoPlayer);

        // не прятать кнопки управления автоматом
        //videoPlayerView.setControllerShowTimeoutMs(0);
        videoPlayerControlView.setShowTimeoutMs(0);

        // Будем прятать элементы управления в полноэкранном режиме при клике по плееру
        // и всегда показывать в режиме с уменьшенным экраном видео с кнопками управления
        // и списком рекомендаций.
        // (вообще, вот так тоже работает: videoPlayerView.setOnClickListener и на клик реагирует
        // не только область видео, но и вся область вокруг)
        videoPlayerView.getVideoSurfaceView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Прячем панель навигации, т.к. в некоторых ситуациях она все равно может появиться
                // (например, если должго задать кнопку выключения телефона и вызвать экран выключения),
                // хотя мы ее и так где только не выключаем и прячем.
                hideNavigationBar();

                toggleFullscreen();
            }
        });

        // клик по видео (см выше) пусть убирает меню и переключает фулскрин,
        // клик по области за пределами видео пусть просто убирает меню без переключения фулскрина
        findViewById(R.id.watch_content_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNavigationBar();
            }
        });

        // https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/Player.Listener.html
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, @Player.PlayWhenReadyChangeReason int reason) {
                // сохранить текущее состояние, если плеер встал на паузу
                if (!playWhenReady) {
                    saveVideoCurrPos();
                }
            }

            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, @Player.DiscontinuityReason int reason) {
                // сохранить текущее состояние, если текущее место проигрывания поменялось
                // по внешней причине, а не в процессе проигрывания
                // (например, пользователь кликнул на ползунке плеера)
                if(reason == Player.DISCONTINUITY_REASON_SEEK) {
                    // Сохранять только в том случае, если переход по действию пользователя (DISCONTINUITY_REASON_SEEK).
                    // При переключении ролика на новый будет вызвано событие DISCONTINUITY_REASON_REMOVE
                    // с новой позицией newPosition равной 0 (её точно нельзя сохранять).
                    // Насчет других возможных вариантов - с ними пока не встречался, если потребуется, добавим.
                    saveVideoCurrPos();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    setPlayerState(PlayerState.LOADED, null);
                } else if (playbackState == Player.STATE_ENDED) {
                    // ролик завершился - переходим к следующему
                    // TODO: сделайть экран с таймаутом секунд на 10, прогрессбаром и кнопкой
                    // перейти сейчас, отменить, играть заново текущий.

                    if (videoList.getAdapter() != null && videoList.getAdapter().getItemCount() > 1) {
                        // переходим на следующее видео по списку рекомендаций
                        final int nextVideoPosition = currentVideoPosition >= videoList.getAdapter().getItemCount() - 1 ?
                                0 : currentVideoPosition + 1;
                        final VideoItem item;
                        if (videoList.getAdapter() instanceof VideoItemPagedListAdapter) {
                            // здесь не случайные рекомендации, а, например, список выдачи по поисковому запросу
                            item = ((VideoItemPagedListAdapter) videoList.getAdapter()).getItem(nextVideoPosition);
                        } else if (videoList.getAdapter() instanceof VideoItemArrayAdapter) {
                            // здесь скорее всего случайные рекомендации
                            item = ((VideoItemArrayAdapter) videoList.getAdapter()).getItem(nextVideoPosition);
                        } else {
                            item = null;
                        }
                        if (item != null) {
                            posMap.put(item.getId(), nextVideoPosition);
                            // перед загрузкой нового видео обнулим текущую позицию
                            playVideoItem(item, true);
                        }
                    }
                }
            }

            @Override
            public void onPlayerError(final PlaybackException error) {
                //  здесь для предотвращения деградации после вот этого коммита
                //  https://github.com/sadr0b0t/yashlang/commit/b89c415ba3d71a0ac81c40f5d54b7fad249eac27
                //  применим логику:
                // - если произошла ошибка при загрузке плеером потока и при этом этот поток -
                //   поток высокого качества, то попробовать загрузить другой поток
                //   (стратегия выбора потока указана в настройках)
                // - если все потоки в списке перепробованы, то показать ошибку
                setPlayerState(PlayerState.LOADING, null);
                boolean tryAnotherStream = false;
                try {
                    final StreamHelper.StreamPair nextPlaybackStreams = StreamHelper.getNextPlaybackStreamPair(
                            WatchVideoActivity.this,
                            currentVideo.getStreamSources().getVideoStreams(),
                            currentVideo.getStreamSources().getAudioStreams(),
                            currentVideo.getPlaybackStreams().getVideoStream());
                    if (currentVideo.getPlaybackStreams().getVideoStream() != null &&
                            nextPlaybackStreams.getVideoStream() != null &&
                            !nextPlaybackStreams.getVideoStream().getUrl().equals(currentVideo.getPlaybackStreams().getVideoStream().getUrl())) {
                        currentVideo.setPlaybackStreams(nextPlaybackStreams);
                        updateControlsValues();
                        tryAnotherStream = true;
                        // показать прогресс загрузки потока
                        setPlayerState(PlayerState.LOADING, null);
                        playVideoStream(
                                currentVideo.getPlaybackStreams().getVideoStream().getUrl(),
                                (currentVideo.getPlaybackStreams().getAudioStream() != null ? currentVideo.getPlaybackStreams().getAudioStream().getUrl() : null),
                                currentVideo.getPausedAt(),
                                !WatchVideoActivity.this.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED));
                    }
                } catch (Exception e) {
                }

                if (!tryAnotherStream) {
                    setPlayerState(PlayerState.ERROR, error.getMessage());
                }
            }
        });

        // Панель ошибки загрузки видео
        videoPlayerErrorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNavigationBar();
            }
        });

        reloadOnErrorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionReload();
            }
        });

        // Панель - ролик закружен, но потоки не выбраны
        videoPlayerNothingToPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNavigationBar();
            }
        });

        selectStreamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionSelectStreams();
            }
        });


        // Панель - прогресс загрузки видео
        videoPlayerLoadingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNavigationBar();
                toggleFullscreen();
            }
        });

        streamInfoTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionSelectStreams();
            }
        });

        // Рекомендации
        // set a LinearLayoutManager with default vertical orientation
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(
                getApplicationContext(), RecyclerView.HORIZONTAL, false);
        videoList.setLayoutManager(linearLayoutManager);
        videoList.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                // чтобы картинки и текст не сбивались в кучку при быстрой промотке
                // см: https://github.com/sadr0b0t/yashlang/issues/129
                return true;
            }
        });


        final RecommendationsMode recommendationsMode = super.getIntent().hasExtra(PARAM_RECOMMENDATIONS_MODE) ?
                (RecommendationsMode) super.getIntent().getSerializableExtra(PARAM_RECOMMENDATIONS_MODE) :
                RecommendationsMode.RANDOM;
        switch (recommendationsMode) {
            case SEARCH_STR: {
                final String searchStr = super.getIntent().getStringExtra(PARAM_SEARCH_STR);
                final boolean shuffle = super.getIntent().getBooleanExtra(PARAM_SHUFFLE, false);

                if(!shuffle) {
                    // будем считать, что в случае с передачей поисковой строки нам передают для
                    // проигрывания первый элемент из поисковой выдачи, поэтому, чтобы кнопка
                    // "следующее видео" не повторяла первый ролик два раза, начнем считать индекс
                    // текущего ролика сразу с 0-ля (т.е. первый элемент списка рекомендаций)
                    // (но, чтобы это сработало, нужно еще ниже положить:
                    // posMap.put(videoItem.getId(), currentVideoPosition))
                    currentVideoPosition = 0;

                    setupVideoListSearchPagedListAdapter(searchStr);
                } else {
                    setupVideoListSearchShuffleArrayAdapter(searchStr);
                }

                break;
            }
            case STARRED: {
                final boolean shuffle = super.getIntent().getBooleanExtra(PARAM_SHUFFLE, false);

                if(!shuffle) {
                    // будем считать, что в случае в режиме "играть любимое" мы выбираем для
                    // проигрывания первый элемент из списка, поэтому, чтобы кнопка
                    // "следующее видео" не повторяла первый ролик два раза, начнем считать индекс
                    // текущего ролика сразу с 0-ля (т.е. первый элемент списка рекомендаций)
                    // (но, чтобы это сработало, нужно еще ниже положить:
                    // posMap.put(videoItem.getId(), currentVideoPosition))
                    currentVideoPosition = 0;

                    setupVideoListStarredPagedListAdapter();
                } else {
                    setupVideoListStarredShuffleArrayAdapter();
                }

                break;
            }
            case PLAYLIST_ID: {
                final long playlistId = super.getIntent().getLongExtra(PARAM_PLAYLIST_ID, PlaylistInfo.ID_NONE);
                final boolean showAll = super.getIntent().getBooleanExtra(PARAM_SHOW_ALL, false);
                final boolean shuffle = super.getIntent().getBooleanExtra(PARAM_SHUFFLE, false);
                final String searchStr = super.getIntent().getStringExtra(PARAM_SEARCH_STR);
                final ConfigOptions.SortBy sortBy = super.getIntent().hasExtra(PARAM_SORT_BY) ?
                        ConfigOptions.SortBy.valueOf(super.getIntent().getStringExtra(PARAM_SORT_BY)) : null;
                final boolean sortDirAsc = super.getIntent().getBooleanExtra(PARAM_SORT_DIR_ASCENDING, false);

                if(!shuffle) {
                    // будем считать, что в случае в режиме "играть плейлист" мы выбираем для
                    // проигрывания первый элемент из списка, поэтому, чтобы кнопка
                    // "следующее видео" не повторяла первый ролик два раза, начнем считать индекс
                    // текущего ролика сразу с 0-ля (т.е. первый элемент списка рекомендаций)
                    // (но, чтобы это сработало, нужно еще ниже положить:
                    // posMap.put(videoItem.getId(), currentVideoPosition))
                    currentVideoPosition = 0;

                    setupVideoListPlaylistPagedListAdapter(playlistId, showAll, searchStr, sortBy, sortDirAsc);
                } else {
                    setupVideoListPlaylistShuffleArrayAdapter(playlistId, searchStr);
                }

                break;
            }
            case PLAYLIST_URL: {
                final String playlistUrl = super.getIntent().getStringExtra(PARAM_PLAYLIST_URL);

                setupVideoListPlaylistOnlinePagedListAdapter(playlistUrl);

                break;
            }
            case PLAYLIST_NEW: {
                final long playlistId = super.getIntent().getLongExtra(PARAM_PLAYLIST_ID, PlaylistInfo.ID_NONE);

                setupVideoListPlaylistNewPagedListAdapter(playlistId);

                break;
            }
            case ALL_NEW: {
                setupVideoListAllNewPagedListAdapter();

                break;
            }
            case RANDOM: {
                setupVideoListRandomArrayAdapter();

                break;
            }
            //case OFF:
            //default:
        }

        // загружаем видео
        // если передан параметр videoId, то загружаем видео по id из базы, если videoId
        // нет, но есть videoYtId, то используем его
        final long videoId = super.getIntent().getLongExtra(PARAM_VIDEO_ID, VideoItem.ID_NONE);
        final String videoItemUrl = super.getIntent().getStringExtra(PARAM_VIDEO_ITEM_URL);
        if (videoId != VideoItem.ID_NONE) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final VideoItem videoItem = VideoDatabase.getDbInstance(
                            WatchVideoActivity.this).videoItemDao().getById(videoId);
                    posMap.put(videoItem.getId(), currentVideoPosition);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            playVideoItem(videoItem, false);
                        }
                    });
                }
            }).start();
        } else if (videoItemUrl != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // грузим информацию из онлайна
                    VideoItem _videoItem;
                    try {
                        _videoItem = ContentLoader.getInstance().fetchVideoItem(videoItemUrl);
                    } catch (ExtractionException | IOException e) {
                        _videoItem = null;
                        //e.printStackTrace();
                    }

                    final VideoItem videoItem = _videoItem;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            playVideoItem(videoItem, false);
                        }
                    });
                }
            }).start();
        }

        // настройки видимости элементов управления
        // (одного этого вызова в onCreate будет не достаточно, т.к.
        // видимость некоторых элементов управления зависит от наличия элементов в списке рекомендаций,
        // а они могут загружаться в фоне позже, чем мы сюда попадем)
        updateControlsVisibility();
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Экран повернули - нужно загрузить новый лэйаут. Придется заново выставить
        // ссылки на элементы интерфейса, но постараемся по максимуму перетащить
        // их состояния и содержимое без перезагрузки.

        // Плеер можно перекинуть прямо во время проигрывания
        final Player exoPlayer = videoPlayerView.getPlayer();
        // Адаптер с рекомендациями тоже получится перекинуть
        final RecyclerView.Adapter videoListAdapter = videoList.getAdapter();

        // Новый лэйаут
        setContentView(R.layout.activity_watch_video);

        videoPlayerView = findViewById(R.id.video_player_view);
        audioPlayerView = findViewById(R.id.audio_player_view);
        audioPlayerThumbImg = findViewById(R.id.audio_player_thumb_img);
        videoPlayerControlView = findViewById(R.id.video_player_control_view);
        streamInfoTxt = findViewById(R.id.stream_info_txt);
        prevVideoBtn = findViewById(R.id.prev_video_btn);
        nextVideoBtn = findViewById(R.id.next_video_btn);

        videoPlayerErrorView = findViewById(R.id.video_player_error_view);
        videoLoadErrorTxt = findViewById(R.id.video_load_error_txt);
        reloadOnErrorBtn = findViewById(R.id.reload_btn);

        videoPlayerNothingToPlayView = findViewById(R.id.video_player_nothing_to_play_view);
        selectStreamBtn = findViewById(R.id.select_stream_btn);

        videoPlayerLoadingView = findViewById(R.id.video_player_loading_view);

        videoList = findViewById(R.id.video_recommend_list);

        toolbar = findViewById(R.id.toolbar);

        // https://developer.android.com/training/appbar
        // https://www.vogella.com/tutorials/AndroidActionBar/article.html#custom-views-in-the-action-bar
        setSupportActionBar(toolbar);
        // кнопка "Назад" на акшенбаре
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                // Прячем панель навигации, т.к. при выборе меню на акшенбаре она появляется опять.
                hideNavigationBar();
            }
        });

        prevVideoBtn.setEnabled(playbackHistory.size() > 1);
        prevVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playbackHistory.size() > 1) {
                    playbackHistory.pop();
                    // здесь снимаем ролик с вершины, но он там снова сразу окажется в playVideoItem
                    playVideoItem(playbackHistory.pop(), false);

                    prevVideoBtn.setEnabled(playbackHistory.size() > 1);
                    updateControlsVisibility();
                }
            }
        });

        nextVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoList.getAdapter() == null) {
                    // по-хорошему, мы не должны сюда попасть, т.к. в этом случае кнопка будет скрыта
                    return;
                }
                // переходим на следующее видео по списку рекомендаций
                final int nextVideoPosition = currentVideoPosition >= videoList.getAdapter().getItemCount() - 1 ?
                        0 : currentVideoPosition + 1;
                final VideoItem item;
                if (videoList.getAdapter() instanceof VideoItemPagedListAdapter) {
                    // здесь не случайные рекомендации, а, например, список выдачи по поисковому запросу
                    item = ((VideoItemPagedListAdapter) videoList.getAdapter()).getItem(nextVideoPosition);
                } else if (videoList.getAdapter() instanceof VideoItemArrayAdapter) {
                    // здесь скорее всего случайные рекомендации
                    item = ((VideoItemArrayAdapter) videoList.getAdapter()).getItem(nextVideoPosition);
                } else {
                    item = null;
                }
                if (item != null) {
                    posMap.put(item.getId(), nextVideoPosition);
                    playVideoItem(item, false);
                }
            }
        });

        // Плеер
        videoPlayerView.requestFocus();
        videoPlayerView.setPlayer(exoPlayer);

        // контроллер отдельно, чтобы красиво добавить справа и слева от плеера кнопки назад и вперед
        videoPlayerView.setUseController(false);
        videoPlayerControlView.setPlayer(exoPlayer);

        // не прятать кнопки управления автоматом
        //videoPlayerView.setControllerShowTimeoutMs(0);
        videoPlayerControlView.setShowTimeoutMs(0);

        // Будем прятать элементы управления в полноэкранном режиме при клике по плееру
        // и всегда показывать в режиме с уменьшенным экраном видео с кнопками управления
        // и списком рекомендаций.
        // (вообще, вот так тоже работает: videoPlayerView.setOnClickListener и на клик реагирует
        // не только область видео, но и вся область вокруг)
        videoPlayerView.getVideoSurfaceView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Прячем панель навигации, т.к. в некоторых ситуациях она все равно может появиться
                // (например, если должго задать кнопку выключения телефона и вызвать экран выключения),
                // хотя мы ее и так где только не выключаем и прячем.
                hideNavigationBar();

                toggleFullscreen();
            }
        });

        // клик по видео (см выше) пусть убирает меню и переключает фулскрин,
        // клик по области за пределами видео пусть просто убирает меню без переключения фулскрина
        findViewById(R.id.watch_content_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNavigationBar();
            }
        });

        // Панель ошибки загрузки видео
        videoPlayerErrorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNavigationBar();
            }
        });

        reloadOnErrorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionReload();
            }
        });

        // Панель - ролик закружен, но потоки не выбраны
        videoPlayerNothingToPlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNavigationBar();
            }
        });

        selectStreamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionSelectStreams();
            }
        });

        // Панель - прогресс загрузки видео
        videoPlayerLoadingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNavigationBar();
                toggleFullscreen();
            }
        });

        streamInfoTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionSelectStreams();
            }
        });

        // Рекомендации
        // set a LinearLayoutManager with default vertical orientation
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(
                getApplicationContext(), RecyclerView.HORIZONTAL, false);
        videoList.setLayoutManager(linearLayoutManager);
        videoList.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                // чтобы картинки и текст не сбивались в кучку при быстрой промотке
                // см: https://github.com/sadr0b0t/yashlang/issues/129
                return true;
            }
        });
        videoList.setAdapter(videoListAdapter);

        // Режим полного или неполного экрана
        setFullscreen(stateFullscreen);

        // настройки видимости элементов управления
        // будет вызвано из setFullscreen(stateFullscreen);
        //updateControlsVisibility();

        // показать информацию о ролике
        updateControlsValues();

        // видео загружено, загружается или ошибка
        setPlayerState(playerState, videoLoadErrorMsg);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // https://developer.android.com/training/appbar/action-views.html

        // Создаём меню заново для каждого нового загруженного видео, при загрузке нового видео
        // в playVideoItem вызываем invalidateOptionsMenu.
        if (currentVideo != null) {

            toolbar.inflateMenu(R.menu.watch_video_actions);

            if (currentVideo.getId() == VideoItem.ID_NONE) {
                toolbar.getMenu().findItem(R.id.action_star).setVisible(false);
                toolbar.getMenu().findItem(R.id.action_blacklist).setVisible(false);
                toolbar.getMenu().findItem(R.id.action_download_streams).setVisible(false);
            } else {
                starredCheck = (CheckBox) toolbar.getMenu().findItem(R.id.action_star).getActionView();
                starredCheck.setButtonDrawable(android.R.drawable.btn_star);

                starredCheck.setChecked(currentVideo.isStarred());
                starredCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                        if (currentVideo != null && currentVideo.getId() != VideoItem.ID_NONE) {
                            final VideoItem _currentVideo = currentVideo;
                            VideoItemActions.actionSetStarred(WatchVideoActivity.this, currentVideo.getId(), isChecked,
                                    new VideoItemActions.OnVideoStarredChangeListener() {
                                        @Override
                                        public void onVideoStarredChange(final long videoId, final boolean starred) {
                                            // обновим кэш
                                            _currentVideo.setStarred(starred);

                                            // и еще обновим кэш, если этот же ролик вдруг есть в списке предыдущих видео
                                            // (там ролик будет тот же, а объект VideoItem - другой)
                                            for (final VideoItem item : playbackHistory) {
                                                if (item.getId() == videoId) {
                                                    item.setStarred(starred);
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                });
            }

            if (currentVideo.getPlaylistId() == PlaylistInfo.ID_NONE) {
                toolbar.getMenu().findItem(R.id.action_copy_playlist_name).setVisible(false);
                toolbar.getMenu().findItem(R.id.action_copy_playlist_url).setVisible(false);
            }

            toolbar.setOnMenuItemClickListener(
                    new Toolbar.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            return onOptionsItemSelected(item);
                        }
                    });
        }

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // экран ушел на задний план: поставить на паузу
        // (текущая позиция будет сохранена в обработчике события постановки на паузу)
        videoPlayerView.getPlayer().setPlayWhenReady(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // В onResume, т.к. после сворачивания и разворачивания приложения (или после выключения и
        // включения экрана) панель навигации появляется опять (еще она появляется при выборе меню
        // на акшенбаре).
        hideNavigationBar();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        videoPlayerView.getPlayer().release();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_copy_video_name: {
                VideoItemActions.actionCopyVideoName(WatchVideoActivity.this, currentVideo);
                break;
            }
            case R.id.action_copy_video_url: {
                VideoItemActions.actionCopyVideoUrl(WatchVideoActivity.this, currentVideo);
                break;
            }
            case R.id.action_copy_playlist_name: {
                VideoItemActions.actionCopyPlaylistName(WatchVideoActivity.this, handler, currentVideo);
                break;
            }
            case R.id.action_copy_playlist_url: {
                VideoItemActions.actionCopyPlaylistUrl(WatchVideoActivity.this, handler, currentVideo);
                break;
            }
            case R.id.action_blacklist: {
                final VideoItem _currentVideo = currentVideo;
                VideoItemActions.actionBlacklist(
                        WatchVideoActivity.this, handler, currentVideo.getId(),
                        new VideoItemActions.OnVideoBlacklistedChangeListener() {
                            @Override
                            public void onVideoBlacklistedChange(final long videoId, final boolean blacklisted) {
                                // обновим кэш
                                _currentVideo.setBlacklisted(true);

                                // и еще обновим кэш, если этот же ролик вдруг есть в списке предыдущих видео
                                // (там ролик будет тот же, а объект VideoItem - другой)
                                for (final VideoItem item : playbackHistory) {
                                    if (item.getId() == videoId) {
                                        item.setBlacklisted(true);
                                    }
                                }
                                // TODO: здесь что-то нужно сделать после добавления видео в блеклист:
                                // удалить из истории, начать проигрывать какое-то другое видео
                                // (какое? первое из рекомендаций? Что если список рекомендаций пуст?),
                                // удалить его из списка рекомендаций (с текущим датасорсом из ROOM
                                // это произойдет автоматом) и т.п.
                            }
                        });
                break;
            }
            case R.id.action_download_streams: {
                actionDownloadStreams();
                break;
            }
            case R.id.action_select_streams: {
                actionSelectStreams();
                break;
            }
            case R.id.action_reload: {
                actionReload();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Избавиться от панели навигации, которая где только не вылезает и потом не прячется сама:
     * - после сворачивания и разворачивания приложения
     * - после выключения и включения экрана
     * - еще она появляется при выборе меню на акшенбаре
     * - если вызвать экран выключения телефона (долгим кликом на кнопку питания)
     */
    private void hideNavigationBar() {
        // Чтобы в полном экране спрятать виртуальную панельку навигации не достаточно флагов в styles.xml
        // https://stackoverflow.com/questions/14178237/setsystemuivisibilitysystem-ui-flag-layout-hide-navigation-does-not-work
        // https://www.vogella.com/tutorials/AndroidActionBar/article.html
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        // с этим флагом акшенбар начнет сверху перекрывать содержимое экрана
                        // (но только если мы не используем Toolbar, а мы используем)
                        //| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        // с этими флагами весь экран перекорежит и на эмуляторе и на телефоне
                        //| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //| View.SYSTEM_UI_FLAG_FULLSCREEN
                        // без этого флага навигация будет опять появляться по первому клику
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    /**
     * Показываем или скрываем элементы управления в зависимости от текущих обстоятельств
     */
    private void updateControlsVisibility() {
        // в первую очередь определим, в полноэкранном режиме или не полноэкранном режиме
        if (stateFullscreen) {
            prevVideoBtn.setVisibility(View.GONE);
            nextVideoBtn.setVisibility(View.GONE);
            videoList.setVisibility(View.GONE);

            getSupportActionBar().hide();

            // сам плеер и элементы управления
            switch (playerState) {
                case EMPTY:
                case ERROR:
                case NOTHING_TO_PLAY:
                    //setFullscreen(false);
                    // в режиме FULLSCREEN мы сюда попасть не должы
                    break;

                case LOADING:
                    videoPlayerView.setVisibility(View.GONE);
                    audioPlayerView.setVisibility(View.GONE);
                    videoPlayerControlView.setVisibility(View.INVISIBLE);
                    streamInfoTxt.setVisibility(View.GONE);
                    videoPlayerLoadingView.setVisibility(View.VISIBLE);
                    videoPlayerErrorView.setVisibility(View.GONE);
                    videoPlayerNothingToPlayView.setVisibility(View.GONE);

                    break;

                case LOADED:
                    if (playerMode == PlayerMode.VIDEO) {
                        videoPlayerView.setVisibility(View.VISIBLE);
                        audioPlayerView.setVisibility(View.GONE);
                    } else { // PlayerMode.AUDIO
                        videoPlayerView.setVisibility(View.GONE);
                        audioPlayerView.setVisibility(View.VISIBLE);
                    }

                    // если делать так, то статус играть/пауза не будет обновляться
                    //videoPlayerControlView.setVisibility(View.GONE);
                    videoPlayerControlView.hide();

                    streamInfoTxt.setVisibility(View.GONE);

                    videoPlayerLoadingView.setVisibility(View.GONE);
                    videoPlayerErrorView.setVisibility(View.GONE);
                    videoPlayerNothingToPlayView.setVisibility(View.GONE);

                    break;
            }
        } else {
            if (videoList.getAdapter() == null || videoList.getAdapter().getItemCount() < 2) {
                prevVideoBtn.setVisibility(View.GONE);
                nextVideoBtn.setVisibility(View.GONE);

                if(super.getIntent().getSerializableExtra(PARAM_RECOMMENDATIONS_MODE) != RecommendationsMode.ALL_NEW) {
                    // спрячем список рекомендаций, даже если в нем будет 1 элемент, т.к.
                    // обычно это будет тот же самый ролик, который сейчас загружен
                    videoList.setVisibility(View.GONE);
                } else {
                    // Но только не в случае, если у нас список рекомендаций - все новые элементы для
                    // всех плейлистов. В этом случае может прозойти совсем не очевидная ситуация:
                    // если в 1-м плейлисте с новыми элементами окажется всего ровно 1 новый элемент,
                    // то движок адаптера вызовет у VideoItemMultPlaylistsOnlyNewOnlineDataSource
                    // только loadIntitial (внутри которого и будет загружен этот 1-й элемент),
                    // после чего, если мы бы спрятали список здесь, loadAfter для загрузки
                    // новых элементов никогда бы не был дальше вызван, т.к. он вызывается по требованию
                    // интерфейса при промотке списка, когда требуется отобразить недостающие элеметы,
                    // а скрытый список не требует ничего отображать. Поэтому мы так и останемся
                    // с единственным элементом, загруженным в loadIntitial, и список рекомендаций
                    // так никогда не отобразится (чтобы отобразить список рекомендаций, нужно загрузить
                    // хотябы еще один элемент, а чтобы загрузить еще один элемент, нужно отобразить
                    // список рекомендаций). Поэтому список рекомендаций в режиме "всё новое" мы
                    // будем отображать всегда, даже если в нем всего один элемент, т.к. мы не знаем,
                    // появится ли там что-то еще или этот первый загруженный элемент вообще единственный.
                    videoList.setVisibility(View.VISIBLE);
                }

            } else {
                prevVideoBtn.setVisibility(playbackHistory.size() > 1 ? View.VISIBLE : View.INVISIBLE);
                nextVideoBtn.setVisibility(View.VISIBLE);
                videoList.setVisibility(View.VISIBLE);
            }

            getSupportActionBar().show();

            // сам плеер и элементы управления
            switch (playerState) {
                case EMPTY:
                    //setFullscreen(false);

                    // обычно этот экран не видно никогда
                    videoPlayerView.setVisibility(View.INVISIBLE);
                    audioPlayerView.setVisibility(View.GONE);
                    videoPlayerControlView.setVisibility(View.GONE);
                    streamInfoTxt.setVisibility(View.GONE);
                    videoPlayerLoadingView.setVisibility(View.GONE);
                    videoPlayerErrorView.setVisibility(View.GONE);
                    videoPlayerNothingToPlayView.setVisibility(View.GONE);

                    break;

                case ERROR:
                    //setFullscreen(false);

                    videoPlayerView.setVisibility(View.GONE);
                    audioPlayerView.setVisibility(View.GONE);
                    videoPlayerControlView.setVisibility(View.GONE);
                    streamInfoTxt.setVisibility(View.GONE);
                    videoPlayerLoadingView.setVisibility(View.GONE);
                    videoPlayerErrorView.setVisibility(View.VISIBLE);
                    videoPlayerNothingToPlayView.setVisibility(View.GONE);

                    break;

                case LOADING:
                    videoPlayerView.setVisibility(View.GONE);
                    audioPlayerView.setVisibility(View.GONE);
                    videoPlayerControlView.setVisibility(View.INVISIBLE);
                    streamInfoTxt.setVisibility(View.VISIBLE);
                    videoPlayerLoadingView.setVisibility(View.VISIBLE);
                    videoPlayerErrorView.setVisibility(View.GONE);
                    videoPlayerNothingToPlayView.setVisibility(View.GONE);

                    break;

                case LOADED:
                    if (playerMode == PlayerMode.VIDEO) {
                        videoPlayerView.setVisibility(View.VISIBLE);
                        audioPlayerView.setVisibility(View.GONE);
                    } else { // PlayerMode.AUDIO
                        videoPlayerView.setVisibility(View.GONE);
                        audioPlayerView.setVisibility(View.VISIBLE);
                    }

                    // если делать так, то статус играть/пауза не будет обновляться
                    //videoPlayerControlView.setVisibility(View.VISIBLE);
                    videoPlayerControlView.show();

                    streamInfoTxt.setVisibility(View.VISIBLE);

                    videoPlayerLoadingView.setVisibility(View.GONE);
                    videoPlayerErrorView.setVisibility(View.GONE);
                    videoPlayerNothingToPlayView.setVisibility(View.GONE);

                    break;

                case NOTHING_TO_PLAY:
                    // ролик загружен, но для проигрывания не выбраны
                    // потоки ни видео, ни аудио
                    // (отличие от состояния ошибки в том, что в этом случае можно
                    // выбрать поток для проигрывания вручшую)

                    videoPlayerView.setVisibility(View.GONE);
                    audioPlayerView.setVisibility(View.GONE);
                    videoPlayerControlView.setVisibility(View.GONE);
                    streamInfoTxt.setVisibility(View.GONE);
                    videoPlayerLoadingView.setVisibility(View.GONE);
                    videoPlayerErrorView.setVisibility(View.GONE);
                    videoPlayerNothingToPlayView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateControlsValues() {
        if (currentVideo != null) {
            getSupportActionBar().setTitle(currentVideo.getName());
            getSupportActionBar().setSubtitle(currentVideo.getUploader());

            if (currentVideo.getPlaybackStreams() != null && currentVideo.getPlaybackStreams().getVideoStream() != null) {
                // есть поток видео
                // если аудио потока нет (ни встроенного, ни отдельного), обозначим ситуацию игконкой "🔇" (unicode: muted speaker)
                // если поток сохранен локально, обозначим иконкой "💾" (unicode: floppy disk)
                // если при этом аудио поток играет онлайн, дополинтельно метка "🎵☁️" (unicode: musical note + cloud) (звук в облаке)
                // если поток видео играет онлайн, а поток аудио - оффлайн, дополнительная метка "🎵💾️"
                //   (unicode: musical note + floppy disk) (звук на дискетке)
                streamInfoTxt.setText(
                        currentVideo.getPlaybackStreams().getVideoStream().getResolution() +
                                (!currentVideo.getPlaybackStreams().getVideoStream().isOnline() ?
                                        " " + getString(R.string.icon_offline) : "") +
                                (currentVideo.getPlaybackStreams().getVideoStream().getStreamType() == StreamCache.StreamType.VIDEO &&
                                        currentVideo.getPlaybackStreams().getAudioStream() == null ? " " + getString(R.string.icon_no_sound) : "") +
                                (!currentVideo.getPlaybackStreams().getVideoStream().isOnline() &&
                                        currentVideo.getPlaybackStreams().getAudioStream() != null &&
                                        currentVideo.getPlaybackStreams().getAudioStream().isOnline() ?
                                        " " + getString(R.string.icon_only_sound) + getString(R.string.icon_online) : "") +
                                (currentVideo.getPlaybackStreams().getVideoStream().isOnline() &&
                                        currentVideo.getPlaybackStreams().getAudioStream() != null &&
                                        !currentVideo.getPlaybackStreams().getAudioStream().isOnline() ?
                                        " " + getString(R.string.icon_only_sound) + getString(R.string.icon_offline) : "")
                );
            } else if (currentVideo.getPlaybackStreams() != null && currentVideo.getPlaybackStreams().getAudioStream() != null) {
                // потока видео нет, но есть поток аудио
                // режим аудио-плеера: обозначим его иконкой "🎵" (unicode: musical note)
                // (еще вариант: радио, но это, вроде логичнее)
                // если поток сохранен локально, обозначим иконкой "💾" (unicode: floppy disk)
                streamInfoTxt.setText(
                        getString(R.string.icon_only_sound) +
                                (currentVideo.getPlaybackStreams().getAudioStream().isOnline() ? "" : " " + getString(R.string.icon_offline))
                );
            } else {
                // потоки не закружены или загружены, но не выбраны (скорее всего в этом случае
                // поле всё равно будет сктрыто)
                streamInfoTxt.setText("");
            }

            // будет видно только в режиме проигрывания адио без видео
            if (currentVideo.getThumbBitmap() != null) {
                audioPlayerThumbImg.setImageBitmap(currentVideo.getThumbBitmap());
            } else {
                audioPlayerThumbImg.setImageResource(R.drawable.ic_yashlang_thumb);
            }
        } else {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setSubtitle("");
            streamInfoTxt.setText("");

            audioPlayerThumbImg.setImageResource(R.drawable.ic_yashlang_thumb);
        }

        prevVideoBtn.setEnabled(playbackHistory.size() > 1);
        videoLoadErrorTxt.setText(videoLoadErrorMsg);
    }

    private void setFullscreen(final boolean fullscreen) {
        stateFullscreen = fullscreen;
        updateControlsVisibility();
        if (stateFullscreen) {
            // продолжить играть, если была пауза
            videoPlayerView.getPlayer().setPlayWhenReady(true);
        }
    }

    private void toggleFullscreen() {
        setFullscreen(!stateFullscreen);
    }

    private void setPlayerState(final PlayerState playerState, final String errorMsg) {
        this.playerState = playerState;
        videoLoadErrorMsg = playerState == PlayerState.ERROR ? errorMsg : "";

        updateControlsValues();

        if (playerState == PlayerState.EMPTY || playerState == PlayerState.ERROR || playerState == PlayerState.NOTHING_TO_PLAY) {
            setFullscreen(false);
            // будет вызвано внутри setFullscreen
            // updateControlsVisibility();
        } else {
            updateControlsVisibility();
        }
    }

    /**
     * Сохраним текущую позицию видео в базу
     */
    private void saveVideoCurrPos() {
        if (currentVideo != null && playerState == PlayerState.LOADED) {
            // сохраним переменные здесь, чтобы потом спокойно их использовать внутри потока
            // и не бояться, что текущее видео будет переключено до того, как состояние сохранится
            final VideoItem _currentVideo = currentVideo;
            final long _currentPos = videoPlayerView.getPlayer().getCurrentPosition();
            // для текущего кэша, да
            _currentVideo.setPausedAt(_currentPos);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    VideoDatabase.getDbInstance(WatchVideoActivity.this).
                            videoItemDao().setPausedAt(_currentVideo.getId(), _currentPos);
                }
            }).start();
        }
    }

    /**
     * Обнулить текущую позицию видео в базе
     */
    private void resetVideoCurrPos() {
        if (currentVideo != null) {
            // сохраним переменные здесь, чтобы потом спокойно их использовать внутри потока
            // и не бояться, что текущее видео будет переключено до того, как состояние сохранится
            final VideoItem _currentVideo = currentVideo;
            // для текущего кэша, да
            _currentVideo.setPausedAt(0);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    VideoDatabase.getDbInstance(WatchVideoActivity.this).
                            videoItemDao().setPausedAt(_currentVideo.getId(), 0);
                }
            }).start();
        }
    }

    /**
     * Начать проигрывание нового ролика - показать информацию о видео, решить вопросы
     * с сохранением позиций предыдущего видео, стеком истории проигрывания и т.п.
     */
    private void playVideoItem(final VideoItem videoItem, final boolean resetCurrPos) {
        // сбросим или сохраним текущую позицию предыдущего видео
        if (resetCurrPos) {
            resetVideoCurrPos();
        } else {
            saveVideoCurrPos();
        }
        // остановим старое видео, если оно играло
        playVideoStream(null, null, 0, false);

        // загружаем новое видео
        setPlayerState(PlayerState.LOADING, null);

        currentVideo = videoItem;
        currentVideoPosition = posMap.containsKey(videoItem.getId()) ? posMap.get(videoItem.getId()) : -1;
        // можно было бы переместить в updateControlsValues, но не обязательно, т.к. это скролл логично делать
        // один раз во время выбора нового ролика, а не каждый раз при обновлении состояния экрана
        if (currentVideoPosition != -1) {
            videoList.scrollToPosition(currentVideoPosition);
        }
        if (videoItem != null) {
            playbackHistory.push(videoItem);

            // обновить меню - в onCreateOptionsMenu()
            invalidateOptionsMenu();

            // теперь то, что в фоне
            videoLoaderExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    // посчитать просмотр (для ролика, загруженного из базы)
                    if (videoItem.getId() != VideoItem.ID_NONE) {
                        VideoDatabase.getDbInstance(WatchVideoActivity.this).
                                videoItemDao().countView(videoItem.getId());
                    }

                    loadVideoItem(videoItem);
                }
            });

            // если иконка видео не заргужена, загрузим её здесь на всякий случай отдельным потоком,
            // это может пригодиться в режиме проигрывания потока аудио без видео.
            // неудачная загрузка иконки не является критичное проблемой, поэтому не будем
            // вставлять ее в основной поток загрузки
            if (videoItem.getThumbBitmap() == null) {
                videoThumbLoaderExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final Bitmap thumb =
                                VideoThumbManager.getInstance().loadVideoThumb(WatchVideoActivity.this, videoItem);
                        videoItem.setThumbBitmap(thumb);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                updateControlsValues();
                            }
                        });
                    }
                });
            }
        }

        // новый текущий ролик - обновить состояние элементов управления
        updateControlsVisibility();
        // показать информацию о ролике
        updateControlsValues();
    }

    /**
     * Загрузка контента видео - выбранного ролика, здесь касается только области проигрывания, т.е. виджет плеера.
     * Время выполнения не определено, т.к. выполняет сетевые операции, поэтому запускать нужно в фоновом потоке.
     *
     * @param videoItem
     */
    private void loadVideoItem(final VideoItem videoItem) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // для начала остановим проигрывание текущего видео,
                // чтобы оно не играло в фоне во время загрузки
                // (если его не остановили перед этим)
                playVideoStream(null, null, 0, false);

                // теперь покажем экран загрузки
                setPlayerState(PlayerState.LOADING, null);
            }
        });

        // Вообще, наверное, хорошо делать эту операцию после того, как плеер перейдет в состояние
        // "загружаем", т.е. создать новый фоновый поток внутри handler.post выше. Но, если handler.post
        // отправляет задачи в синхронную очередь для выполнения одна за одной, то задача в handler.post
        // ниже будет выполнена в любом случае после задачи handler.post выше, поэтому проблемы

        // загрузить поток видео
        final StreamHelper.StreamSources streamSources = StreamHelper.fetchStreams(this, videoItem);
        if (streamSources.getVideoStreams().size() > 0 || streamSources.getAudioStreams().size() > 0) {
            StreamHelper.sortVideoStreamsDefault(streamSources.getVideoStreams());
            StreamHelper.sortAudioStreamsDefault(streamSources.getAudioStreams());
            final StreamHelper.StreamPair playbackStreams = StreamHelper.getNextPlaybackStreamPair(
                    this, streamSources.getVideoStreams(), streamSources.getAudioStreams(), null);
            videoItem.setStreamSources(streamSources);
            videoItem.setPlaybackStreams(playbackStreams);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    // пока загружали информацию о видео, пользователь мог кликнуть на загрузку нового ролика,
                    // в этом случае уже нет смысла загружать в плеер этот ролик на долю секунды
                    // или на то время, пока загружается новый ролик, поэтому здесь просто ничего не делаем,
                    // а плеер останется в статусе "LOADING" до тех пор, пока не будет загружен новый ролик
                    if (videoItem == currentVideo) {

                        // т.к. загрузка видео осуществляется в фононовом потоке, мы можем сюда попасть
                        // в такой ситуации, когда пользователь кликнул на загрузку видео, а потом
                        // сразу свернул приложение - в этом случае ролик начнет проигрывание в фоне,
                        // а пользователь услышит его звук и ему придется вернуться в приложение, чтобы
                        // поставить плеер на паузу.
                        // по этой причине мы здесь проверяем, является ли экран с плеером активным
                        // (см: https://stackoverflow.com/questions/5446565/android-how-do-i-check-if-activity-is-running/25722319 )
                        // и если не является, то загружать видео, но не начинать его проигрывание
                        // сразу после загрузки.
                        // https://github.com/sadr0b0t/yashlang/issues/4

                        updateControlsValues();

                        if (videoItem.getPlaybackStreams().getVideoStream() == null && videoItem.getPlaybackStreams().getAudioStream() == null) {
                            // здесь нас тоже скорее всего не будет, т.к. в автоматическом режиме
                            // если есть потоки видео или адио, что-то из них будет выбрано
                            setPlayerState(PlayerState.NOTHING_TO_PLAY, null);
                        } else {
                            try {
                                playVideoStream(
                                        (videoItem.getPlaybackStreams().getVideoStream() != null ? videoItem.getPlaybackStreams().getVideoStream().getUrl() : null),
                                        (videoItem.getPlaybackStreams().getAudioStream() != null ? videoItem.getPlaybackStreams().getAudioStream().getUrl() : null),
                                        videoItem.getPausedAt(),
                                        !WatchVideoActivity.this.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED));
                            } catch (Exception ex) {
                                // в принципе, мы сюда не должны попасть никогда. Возможно, был повод
                                // поймать RuntimeException в плеере и не упасть.
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            });
        } else {
            if (videoItem == currentVideo) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setPlayerState(
                                PlayerState.ERROR,
                                WatchVideoActivity.this.getString(R.string.no_playback_streams_for_video) +
                                        (streamSources.problems.size() > 0 ? "\n" + streamSources.problems.get(0).getMessage() : ""));
                    }
                });
            }
        }
    }

    /**
     * Собственно, запустить на проигрывание видеопоток по известному адресу
     *
     * @param streamUrl адрес потока видео (должен быть с форматом ExoPlayer),
     *                  может содержать или не содержать дорожку аудио
     *                  если null, остановить проигрывание текущего ролика, если он уже был загружен
     * @param audioStreamUrl адрес потока аудио. Нужен, если у потока видео нет дорожки аудио.
     *                  Отправить null, если у потока видео есть совмещенная дорожка аудио.
     * @param seekTo    начать проигрывание с указанной позиции
     * @param paused    false: начать проигрывание сразу после загрузки;
     *                  true: загрузить поток и поставить на паузу
     */
    private void playVideoStream(final String streamUrl, final String audioStreamUrl, final long seekTo, final boolean paused) {
        if (streamUrl == null && audioStreamUrl == null) {
            // остановить проигрывание текущего ролика, если был загружен
            videoPlayerView.getPlayer().stop();
            videoPlayerView.getPlayer().clearMediaItems();
        } else {
            // https://exoplayer.dev/
            // https://github.com/google/ExoPlayer

            // датасорсы к видео в плеере NewPipe:
            // - про продолжение с установленной позиции в коде не вижу или не нашел
            // - (как играть видео конкретно с ютюба не вижу тоже, там ацкий ООП)
            // - короче, толку от них ноль, пусть будут пока ссылки для справки
            // https://github.com/TeamNewPipe/NewPipe/blob/master/app/src/main/java/org/schabi/newpipe/player/helper/PlayerDataSource.java
            // https://github.com/TeamNewPipe/NewPipe/blob/master/app/src/main/java/org/schabi/newpipe/player/resolver/PlaybackResolver.java

            final MediaSource mediaSource;
            final MediaSource videoSource;
            final MediaSource audioSource;

            if (streamUrl != null) {
                final Uri mp4VideoUri = Uri.parse(streamUrl);
                videoSource = new ProgressiveMediaSource.Factory(videoDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(mp4VideoUri));
            } else {
                videoSource = null;
            }
            if (audioStreamUrl != null) {
                final Uri mp3AudioUri = Uri.parse(audioStreamUrl);
                audioSource = new ProgressiveMediaSource.Factory(videoDataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(mp3AudioUri));
            } else {
                audioSource = null;
            }

            if (videoSource != null && audioSource == null) {
                mediaSource = videoSource;
                playerMode = PlayerMode.VIDEO;
            } else if (videoSource == null && audioSource != null) {
                mediaSource = audioSource;
                playerMode = PlayerMode.AUDIO;
            } else {
                // videoSource != null && audioSource != null
                // (оба null буть не могут, т.к. этот случай отсекли еще выше)
                // совместить дорожку аудио и видео
                // https://stackoverflow.com/questions/58404056/exoplayer-play-an-audio-stream-and-a-video-stream-synchronously

                mediaSource = new MergingMediaSource(videoSource, audioSource);
                playerMode = PlayerMode.VIDEO;
            }

            // Поставим на паузу старое видео, пока готовим новое
            if (videoPlayerView.getPlayer().getPlaybackState() != Player.STATE_ENDED) {
                // Если ставить на паузу здесь после того, как плеер встал на паузу сам, закончив
                // играть видео, получим здесь второе событие STATE_ENDED, поэтому нам нужна здесь
                // специальная проверка.
                // При этом значение getPlayWhenReady() останется true, поэтому проверяем именно состояние.
                // https://github.com/google/ExoPlayer/issues/2272
                videoPlayerView.getPlayer().setPlayWhenReady(false);
            }

            // Prepare the player with the source
            ((SimpleExoPlayer) videoPlayerView.getPlayer()).setMediaSource(mediaSource);
            videoPlayerView.getPlayer().prepare();

            // Укажем текущую позицию сразу при загрузке видео
            // (в коментах что-то пишут что-то про датасорсы, которые поддерживают или не поддерживают
            // переходы seek при загрузке, похоже, что это фигня - просто делаем seek сразу после загрузки)
            // Exoplayer plays new Playlist from the beginning instead of provided position
            // https://github.com/google/ExoPlayer/issues/4375
            // How to load stream in the desired position? #2197
            // https://github.com/google/ExoPlayer/issues/2197
            // в этом месте нормлаьный duration еще не доступен, поэтому его не проверяем
            //if(seekTo > 0 && seekTo < videoPlayerView.getPlayer().getDuration()) {
            if (seekTo > 0) {
                // на 5 секунд раньше
                videoPlayerView.getPlayer().seekTo(seekTo - 5000 > 0 ? seekTo - 5000 : 0);
            }
            videoPlayerView.getPlayer().setPlayWhenReady(!paused);

            // статус LOADED хорошо убирать после загрузки потока перед началом проигрывания
            // (произошло событие onPlaybackStateChanged:Player.STATE_READY)
            //setPlayerState(PlayerState.LOADED, null);
        }
    }

    /**
     * Выбрать видеопоток для текущего ролика
     */
    private void actionSelectStreams() {
        if (currentVideo != null) {
            videoPlayerView.getPlayer().setPlayWhenReady(false);
            VideoItemActions.actionSelectStreams(this, handler, currentVideo,
                    new VideoItemActions.StreamDialogListener() {
                        @Override
                        public void onClose() {
                            // Прячем панель навигации, т.к. при выборе меню она появляется опять.
                            hideNavigationBar();
                        }

                        @Override
                        public void onStreamsSelected(final StreamHelper.StreamInfo videoStream, final StreamHelper.StreamInfo audioStream) {
                            // сохраним переменные здесь, чтобы потом спокойно их использовать внутри потока
                            // и не бояться, что текущее видео будет переключено до того, как состояние сохранится
                            final VideoItem _currentVideo = currentVideo;
                            final long _currentPos = videoPlayerView.getPlayer().getCurrentPosition();
                            // для текущего кэша, да
                            if (currentVideo != null && playerState == PlayerState.LOADED) {
                                currentVideo.setPausedAt(_currentPos);
                            }

                            setPlayerState(PlayerState.LOADING, null);
                            // сохраним текущую позицию (если она больше нуля) в б/д и загрузим
                            // видео заново - обе операции в фоновом потоке
                            videoLoaderExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    // если за время запуска потока видео успели переключить, всё отменяем
                                    if (_currentVideo == currentVideo) {
                                        if (_currentVideo.getId() != VideoItem.ID_NONE) {
                                            if (playerState == PlayerState.LOADED) {
                                                // сохраним текущую позицию только в том случае, если ролик был загружен
                                                // (может быть ситуация, когда мы переключились на видео с ранее
                                                // сохраненной позицией, а оно не загрузилось, тогда бы у нас
                                                // сбросилась старая сохраненная позиция, а это не хорошо)
                                                VideoDatabase.getDbInstance(WatchVideoActivity.this).
                                                        videoItemDao().setPausedAt(_currentVideo.getId(), _currentPos);
                                            }
                                        }

                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                // сохраним выбранное вручную качество в настройки
                                                if (videoStream != null) {
                                                    ConfigOptions.setVideoStreamLastSelectedRes(WatchVideoActivity.this, videoStream.getResolution());
                                                }
                                                _currentVideo.setPlaybackStreams(new StreamHelper.StreamPair(videoStream, audioStream));
                                                if (videoStream == null && audioStream == null) {
                                                    setPlayerState(PlayerState.NOTHING_TO_PLAY, null);
                                                } else {
                                                    updateControlsValues();
                                                    playVideoStream(
                                                            (videoStream != null ? videoStream.getUrl() : null),
                                                            (audioStream != null ? audioStream.getUrl() : null),
                                                            _currentVideo.getPausedAt(),
                                                            !WatchVideoActivity.this.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED));
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
        }
    }

    /**
     * Загрузить видео для просмотра оффлайн.
     */
    private void actionDownloadStreams() {
        if (currentVideo != null) {
            videoPlayerView.getPlayer().setPlayWhenReady(false);
            VideoItemActions.actionDownloadStreams(this, handler, currentVideo,
                    new VideoItemActions.StreamDialogListener() {
                        @Override
                        public void onClose() {
                            // Прячем панель навигации, т.к. при выборе меню она появляется опять.
                            hideNavigationBar();
                        }

                        @Override
                        public void onStreamsSelected(StreamHelper.StreamInfo videoStream, StreamHelper.StreamInfo audioStream) {
                        }
                    });
        }
    }

    /**
     * Загрузить заново видеопоток для текущего ролика
     */
    private void actionReload() {
        if (currentVideo != null) {
            if (currentVideo.getId() != VideoItem.ID_NONE) {
                // загрузить поток видео заново (иногда после разрыва соединения
                // видео может перестать загружаться и появление соединения процесс
                // не возобновляет)

                // сохраним переменные здесь, чтобы потом спокойно их использовать внутри потока
                // и не бояться, что текущее видео будет переключено до того, как состояние сохранится
                final VideoItem _currentVideo = currentVideo;
                final long _currentPos = videoPlayerView.getPlayer().getCurrentPosition();
                // для текущего кэша, да
                if (currentVideo != null && playerState == PlayerState.LOADED) {
                    currentVideo.setPausedAt(_currentPos);
                }
                // сохраним текущую позицию (если она больше нуля) в б/д и загрузим
                // видео заново - обе операции в фоновом потоке
                videoLoaderExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        // если за время запуска потока видео успели переключить, всё отменяем
                        if (_currentVideo != null && _currentVideo == currentVideo) {
                            if (playerState == PlayerState.LOADED) {
                                // сохраним текущую позицию только в том случае, если ролик был загружен
                                // (может быть ситуация, когда мы переключились на видео с ранее
                                // сохраненной позицией, а оно не загрузилось, тогда бы у нас
                                // сбросилась старая сохраненная позиция, а это не хорошо)
                                VideoDatabase.getDbInstance(WatchVideoActivity.this).
                                        videoItemDao().setPausedAt(_currentVideo.getId(), _currentPos);
                            }

                            loadVideoItem(currentVideo);
                        }
                    }
                });
            } else {
                // если видео нет в базе
                final VideoItem _currentVideo = currentVideo;
                final long _currentPos = videoPlayerView.getPlayer().getCurrentPosition();
                // для текущего кэша, да
                if (currentVideo != null && playerState == PlayerState.LOADED) {
                    currentVideo.setPausedAt(_currentPos);
                }
                videoLoaderExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        // если за время запуска потока видео успели переключить, всё отменяем
                        if (_currentVideo != null && _currentVideo == currentVideo) {
                            loadVideoItem(currentVideo);
                        }
                    }
                });
            }
        }
    }

    private void actionVideoContextMenu(final View view, final VideoItem videoItem) {
        final PopupMenu popup = new PopupMenu(WatchVideoActivity.this, view);
        popup.getMenuInflater().inflate(R.menu.video_item_actions, popup.getMenu());
        popup.getMenu().removeItem(R.id.action_play_in_playlist);
        popup.getMenu().removeItem(R.id.action_play_in_playlist_shuffle);

        if (videoItem == null || videoItem.getPlaylistId() == PlaylistInfo.ID_NONE) {
            popup.getMenu().removeItem(R.id.action_copy_playlist_name);
            popup.getMenu().removeItem(R.id.action_copy_playlist_url);
        }

        if (videoItem == null || videoItem.getId() == VideoItem.ID_NONE) {
            popup.getMenu().removeItem(R.id.action_blacklist);
            popup.getMenu().removeItem(R.id.action_download_streams);
        }

        popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                // Прячем панель навигации, т.к. при выборе меню она появляется опять.
                hideNavigationBar();
            }
        });
        popup.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_copy_video_name: {
                                VideoItemActions.actionCopyVideoName(WatchVideoActivity.this, videoItem);
                                break;
                            }
                            case R.id.action_copy_video_url: {
                                VideoItemActions.actionCopyVideoUrl(WatchVideoActivity.this, videoItem);
                                break;
                            }
                            case R.id.action_copy_playlist_name: {
                                VideoItemActions.actionCopyPlaylistName(WatchVideoActivity.this, handler, videoItem);
                                break;
                            }
                            case R.id.action_copy_playlist_url: {
                                VideoItemActions.actionCopyPlaylistUrl(WatchVideoActivity.this, handler, videoItem);
                                break;
                            }
                            case R.id.action_blacklist: {
                                VideoItemActions.actionBlacklist(
                                        WatchVideoActivity.this, handler, videoItem.getId(),
                                        new VideoItemActions.OnVideoBlacklistedChangeListener() {
                                            @Override
                                            public void onVideoBlacklistedChange(final long videoId, final boolean blacklisted) {
                                                // обновим кэш для текущего видео если вдруг так получилось,
                                                // что мы играем сейчас тот самый ролик, на котором кликнули
                                                // в рекомендациях
                                                if (currentVideo != null && currentVideo.getId() == videoItem.getId()) {
                                                    currentVideo.setBlacklisted(blacklisted);
                                                }

                                                // и еще обновим кэш, если этот же ролик вдруг есть в списке предыдущих видео
                                                // (там ролик будет тот же, а объект VideoItem - другой)
                                                for (final VideoItem item : playbackHistory) {
                                                    if (item.getId() == videoId) {
                                                        item.setBlacklisted(blacklisted);
                                                    }
                                                }
                                                // TODO: здесь что-то нужно сделать после добавления видео в блеклист:
                                                // например, удалить из текущего списка рекомендаций
                                            }
                                        });
                                break;
                            }
                            case R.id.action_download_streams: {
                                VideoItemActions.actionDownloadStreams(
                                        WatchVideoActivity.this, handler, videoItem, null);
                                break;
                            }
                        }
                        return true;
                    }
                }
        );
        popup.show();
    }

    /**
     * Случайные рекомандации внизу под основным видео. ArrayAdapter, а не PagedListAdapter
     * потому, что в случае с PagedListAdapter выдача рекомендаций будет автоматом обновляться
     * при каждой записи в базу (например, при переключении видео с сохранением текущей позиции
     * или при клике на кнопку со звездочкой).
     */
    private void setupVideoListRandomArrayAdapter() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<VideoItem> videoItems = VideoDatabase.getDbInstance(WatchVideoActivity.this).
                        videoItemDao().recommendVideos(ConfigOptions.RECOMMENDED_RANDOM_LIM);
                final VideoItemArrayAdapter adapter = new VideoItemArrayAdapter(
                        WatchVideoActivity.this, videoItems, new OnListItemClickListener<VideoItem>() {
                    @Override
                    public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                        posMap.put(videoItem.getId(), position);
                        playVideoItem(videoItem, false);
                    }

                    @Override
                    public boolean onItemLongClick(final View view, final int position, final VideoItem videoItem) {
                        actionVideoContextMenu(view, videoItem);
                        return true;
                    }
                }, null, VideoItemArrayAdapter.ORIENTATION_HORIZONTAL);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        videoList.setAdapter(adapter);
                        // emptyListObserver здесь не сработает (т.к. у нас ArrayAdapter),
                        // обновим видимость элементов управления прямо здесь
                        updateControlsVisibility();
                    }
                });
            }
        }).start();
    }

    private void setupVideoListSearchShuffleArrayAdapter(final String searchStr) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<VideoItem> videoItems = VideoDatabase.getDbInstance(WatchVideoActivity.this).
                        videoItemDao().searchVideosShuffle(searchStr, ConfigOptions.RECOMMENDED_RANDOM_LIM);
                final VideoItemArrayAdapter adapter = new VideoItemArrayAdapter(
                        WatchVideoActivity.this, videoItems, new OnListItemClickListener<VideoItem>() {
                    @Override
                    public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                        posMap.put(videoItem.getId(), position);
                        playVideoItem(videoItem, false);
                    }

                    @Override
                    public boolean onItemLongClick(final View view, final int position, final VideoItem videoItem) {
                        actionVideoContextMenu(view, videoItem);
                        return true;
                    }
                }, null, VideoItemArrayAdapter.ORIENTATION_HORIZONTAL);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        videoList.setAdapter(adapter);
                        // emptyListObserver здесь не сработает (т.к. у нас ArrayAdapter),
                        // обновим видимость элементов управления прямо здесь
                        updateControlsVisibility();
                    }
                });
            }
        }).start();
    }

    private void setupVideoListStarredShuffleArrayAdapter() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<VideoItem> videoItems = VideoDatabase.getDbInstance(WatchVideoActivity.this).
                        videoItemDao().getStarredShuffle(ConfigOptions.RECOMMENDED_RANDOM_LIM);
                final VideoItemArrayAdapter adapter = new VideoItemArrayAdapter(
                        WatchVideoActivity.this, videoItems, new OnListItemClickListener<VideoItem>() {
                    @Override
                    public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                        posMap.put(videoItem.getId(), position);
                        playVideoItem(videoItem, false);
                    }

                    @Override
                    public boolean onItemLongClick(final View view, final int position, final VideoItem videoItem) {
                        actionVideoContextMenu(view, videoItem);
                        return true;
                    }
                }, null, VideoItemArrayAdapter.ORIENTATION_HORIZONTAL);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        videoList.setAdapter(adapter);
                        // emptyListObserver здесь не сработает (т.к. у нас ArrayAdapter),
                        // обновим видимость элементов управления прямо здесь
                        updateControlsVisibility();
                    }
                });
            }
        }).start();
    }

    private void setupVideoListPlaylistShuffleArrayAdapter(final long playlistId, final String searchStr) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<VideoItem> videoItems;
                if(searchStr != null) {
                    videoItems = VideoDatabase.getDbInstance(WatchVideoActivity.this).
                            videoItemDao().getByPlaylistShuffle(playlistId, searchStr, ConfigOptions.RECOMMENDED_RANDOM_LIM);
                } else {
                    videoItems = VideoDatabase.getDbInstance(WatchVideoActivity.this).
                            videoItemDao().getByPlaylistShuffle(playlistId, ConfigOptions.RECOMMENDED_RANDOM_LIM);
                }
                final VideoItemArrayAdapter adapter = new VideoItemArrayAdapter(
                        WatchVideoActivity.this, videoItems, new OnListItemClickListener<VideoItem>() {
                    @Override
                    public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                        posMap.put(videoItem.getId(), position);
                        playVideoItem(videoItem, false);
                    }

                    @Override
                    public boolean onItemLongClick(final View view, final int position, final VideoItem videoItem) {
                        actionVideoContextMenu(view, videoItem);
                        return true;
                    }
                }, null, VideoItemArrayAdapter.ORIENTATION_HORIZONTAL);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        videoList.setAdapter(adapter);
                        // emptyListObserver здесь не сработает (т.к. у нас ArrayAdapter),
                        // обновим видимость элементов управления прямо здесь
                        updateControlsVisibility();
                    }
                });
            }
        }).start();
    }


    private void setupVideoListSearchPagedListAdapter(final String searchStr) {
        if (videoItemsLiveData != null) {
            videoItemsLiveData.removeObservers(this);
        }

        if (videoList.getAdapter() != null) {
            videoList.getAdapter().unregisterAdapterDataObserver(emptyListObserver);
        }

        final VideoItemPagedListAdapter adapter = new VideoItemPagedListAdapter(
                this, new OnListItemClickListener<VideoItem>() {
            @Override
            public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                posMap.put(videoItem.getId(), position);
                playVideoItem(videoItem, false);
            }

            @Override
            public boolean onItemLongClick(View view, int position, VideoItem videoItem) {
                actionVideoContextMenu(view, videoItem);
                return true;
            }
        }, null, VideoItemPagedListAdapter.ORIENTATION_HORIZONTAL);

        // видимость некоторых элементов управления зависит от наличия элементов в
        // списке рекомендаций, а они могут загружаться в фоне
        adapter.registerAdapterDataObserver(emptyListObserver);

        // Initial page size to fetch can also be configured here too
        final PagedList.Config config = new PagedList.Config.Builder().setPageSize(20).build();

        final DataSource.Factory factory = VideoDatabase.getDbInstance(WatchVideoActivity.this).
                videoItemDao().searchVideosDs(searchStr);

        videoItemsLiveData = new LivePagedListBuilder(factory, config).build();

        videoItemsLiveData.observe(this, new Observer<PagedList<VideoItem>>() {
            @Override
            public void onChanged(@Nullable PagedList<VideoItem> videos) {
                adapter.submitList(videos);
            }
        });

        videoList.setAdapter(adapter);
    }

    private void setupVideoListStarredPagedListAdapter() {
        if (videoItemsLiveData != null) {
            videoItemsLiveData.removeObservers(this);
        }

        if (videoList.getAdapter() != null) {
            videoList.getAdapter().unregisterAdapterDataObserver(emptyListObserver);
        }

        final VideoItemPagedListAdapter adapter = new VideoItemPagedListAdapter(
                this, new OnListItemClickListener<VideoItem>() {
            @Override
            public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                posMap.put(videoItem.getId(), position);
                playVideoItem(videoItem, false);
            }

            @Override
            public boolean onItemLongClick(View view, int position, VideoItem videoItem) {
                actionVideoContextMenu(view, videoItem);
                return true;
            }
        }, null, VideoItemPagedListAdapter.ORIENTATION_HORIZONTAL);

        // видимость некоторых элементов управления зависит от наличия элементов в
        // списке рекомендаций, а они могут загружаться в фоне
        adapter.registerAdapterDataObserver(emptyListObserver);

        // Initial page size to fetch can also be configured here too
        final PagedList.Config config = new PagedList.Config.Builder().setPageSize(20).build();

        final DataSource.Factory factory = VideoDatabase.getDbInstance(WatchVideoActivity.this).
                videoItemDao().getStarredDs();

        videoItemsLiveData = new LivePagedListBuilder(factory, config).build();

        videoItemsLiveData.observe(this, new Observer<PagedList<VideoItem>>() {
            @Override
            public void onChanged(@Nullable PagedList<VideoItem> videos) {
                adapter.submitList(videos);
            }
        });

        videoList.setAdapter(adapter);
    }

    private void setupVideoListPlaylistPagedListAdapter(
            final long playlistId, final boolean showAll,
            final String searchStr,
            final ConfigOptions.SortBy sortBy, final boolean sortDirAsc) {
        if (videoItemsLiveData != null) {
            videoItemsLiveData.removeObservers(this);
        }

        if (videoList.getAdapter() != null) {
            videoList.getAdapter().unregisterAdapterDataObserver(emptyListObserver);
        }

        final VideoItemPagedListAdapter adapter = new VideoItemPagedListAdapter(
                this, new OnListItemClickListener<VideoItem>() {
            @Override
            public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                posMap.put(videoItem.getId(), position);
                playVideoItem(videoItem, false);
            }

            @Override
            public boolean onItemLongClick(View view, int position, VideoItem videoItem) {
                actionVideoContextMenu(view, videoItem);
                return true;
            }
        }, null, VideoItemPagedListAdapter.ORIENTATION_HORIZONTAL);

        // видимость некоторых элементов управления зависит от наличия элементов в
        // списке рекомендаций, а они могут загружаться в фоне
        adapter.registerAdapterDataObserver(emptyListObserver);

        // Initial page size to fetch can also be configured here too
        final PagedList.Config config = new PagedList.Config.Builder().setPageSize(20).build();

        final DataSource.Factory factory;
        if(showAll) {
            if (searchStr != null && !searchStr.isEmpty()) {
                factory = VideoDatabase.getDbInstance(WatchVideoActivity.this).videoItemDao().getByPlaylistAllDs(playlistId, searchStr);
            } else {
                factory = VideoDatabase.getDbInstance(WatchVideoActivity.this).videoItemDao().getByPlaylistAllDs(playlistId);
            }
        } else {
            if(sortBy == null) {
                factory = VideoDatabase.getDbInstance(WatchVideoActivity.this).videoItemDao().getByPlaylistDs(playlistId);
            } else if(sortBy == ConfigOptions.SortBy.NAME) {
                if (sortDirAsc) {
                    factory = VideoDatabase.getDbInstance(
                            WatchVideoActivity.this).videoItemDao().getByPlaylistSortByNameAscDs(playlistId, searchStr);
                } else {
                    factory = VideoDatabase.getDbInstance(
                            WatchVideoActivity.this).videoItemDao().getByPlaylistSortByNameDescDs(playlistId, searchStr);
                }
            } else if(sortBy == ConfigOptions.SortBy.DURATION) {
                if(sortDirAsc) {
                    factory = VideoDatabase.getDbInstance(
                            WatchVideoActivity.this).videoItemDao().getByPlaylistSortByDurationAscDs(playlistId, searchStr);
                }else {
                    factory = VideoDatabase.getDbInstance(
                            WatchVideoActivity.this).videoItemDao().getByPlaylistSortByDurationDescDs(playlistId, searchStr);
                }
            } else { // TIME_ADDED
                if(sortDirAsc) {
                    factory = VideoDatabase.getDbInstance(
                            WatchVideoActivity.this).videoItemDao().getByPlaylistSortByTimeAddedAscDs(playlistId, searchStr);
                } else {
                    factory = VideoDatabase.getDbInstance(
                            WatchVideoActivity.this).videoItemDao().getByPlaylistSortByTimeAddedDescDs(playlistId, searchStr);
                }
            }
        }

        videoItemsLiveData = new LivePagedListBuilder(factory, config).build();

        videoItemsLiveData.observe(this, new Observer<PagedList<VideoItem>>() {
            @Override
            public void onChanged(@Nullable PagedList<VideoItem> videos) {
                adapter.submitList(videos);
            }
        });

        videoList.setAdapter(adapter);
    }

    private void setupVideoListPlaylistOnlinePagedListAdapter(final String playlistUrl) {
        if (videoItemsLiveData != null) {
            videoItemsLiveData.removeObservers(this);
        }

        if (videoList.getAdapter() != null) {
            videoList.getAdapter().unregisterAdapterDataObserver(emptyListObserver);
        }

        final VideoItemPagedListAdapter adapter = new VideoItemPagedListAdapter(
                this, new OnListItemClickListener<VideoItem>() {
            @Override
            public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                posMap.put(videoItem.getId(), position);
                playVideoItem(videoItem, false);
            }

            @Override
            public boolean onItemLongClick(View view, int position, VideoItem videoItem) {
                actionVideoContextMenu(view, videoItem);
                return true;
            }
        }, null, VideoItemPagedListAdapter.ORIENTATION_HORIZONTAL);

        // видимость некоторых элементов управления зависит от наличия элементов в
        // списке рекомендаций, а они могут загружаться в фоне
        adapter.registerAdapterDataObserver(emptyListObserver);

        // Initial page size to fetch can also be configured here too
        final PagedList.Config config = new PagedList.Config.Builder().setPageSize(20).build();

        final DataSource.Factory factory =
                new VideoItemOnlineDataSourceFactory(this, playlistUrl, false, null);

        videoItemsLiveData = new LivePagedListBuilder(factory, config).build();

        videoItemsLiveData.observe(this, new Observer<PagedList<VideoItem>>() {
            @Override
            public void onChanged(@Nullable PagedList<VideoItem> videos) {
                adapter.submitList(videos);
            }
        });

        videoList.setAdapter(adapter);
    }

    private void setupVideoListPlaylistNewPagedListAdapter(final long playlistId) {
        if (videoItemsLiveData != null) {
            videoItemsLiveData.removeObservers(this);
        }

        if (videoList.getAdapter() != null) {
            videoList.getAdapter().unregisterAdapterDataObserver(emptyListObserver);
        }

        final VideoItemPagedListAdapter adapter = new VideoItemPagedListAdapter(
                this, new OnListItemClickListener<VideoItem>() {
            @Override
            public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                posMap.put(videoItem.getId(), position);
                playVideoItem(videoItem, false);
            }

            @Override
            public boolean onItemLongClick(View view, int position, VideoItem videoItem) {
                actionVideoContextMenu(view, videoItem);
                return true;
            }
        }, null, VideoItemPagedListAdapter.ORIENTATION_HORIZONTAL);

        // видимость некоторых элементов управления зависит от наличия элементов в
        // списке рекомендаций, а они могут загружаться в фоне
        adapter.registerAdapterDataObserver(emptyListObserver);

        // Initial page size to fetch can also be configured here too
        final PagedList.Config config = new PagedList.Config.Builder().setPageSize(20).build();

        final DataSource.Factory factory =
                new VideoItemOnlyNewOnlineDataSourceFactory(this, playlistId, false, null);

        videoItemsLiveData = new LivePagedListBuilder(factory, config).build();

        videoItemsLiveData.observe(this, new Observer<PagedList<VideoItem>>() {
            @Override
            public void onChanged(@Nullable PagedList<VideoItem> videos) {
                adapter.submitList(videos);
            }
        });

        videoList.setAdapter(adapter);
    }

    private void setupVideoListAllNewPagedListAdapter() {
        // здесь в фоне, т.к. список всех плейлистов получаем из базы данных
        // (вообще, был бы нормальный вариант сделать конструктор
        // VideoItemMultPlaylistsOnlyNewOnlineDataSourceFactory без параметров, в таком случае
        // список всех плейлистов можно было бы извлекать в фоне внутри
        // VideoItemMultPlaylistsOnlyNewOnlineDataSource.loadInitial , но так тоже ок)
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (videoItemsLiveData != null) {
                    videoItemsLiveData.removeObservers(WatchVideoActivity.this);
                }

                if (videoList.getAdapter() != null) {
                    videoList.getAdapter().unregisterAdapterDataObserver(emptyListObserver);
                }

                final VideoItemPagedListAdapter adapter = new VideoItemPagedListAdapter(
                        WatchVideoActivity.this, new OnListItemClickListener<VideoItem>() {
                    @Override
                    public void onItemClick(final View view, final int position, final VideoItem videoItem) {
                        posMap.put(videoItem.getId(), position);
                        playVideoItem(videoItem, false);
                    }

                    @Override
                    public boolean onItemLongClick(View view, int position, VideoItem videoItem) {
                        actionVideoContextMenu(view, videoItem);
                        return true;
                    }
                }, null, VideoItemPagedListAdapter.ORIENTATION_HORIZONTAL);

                // видимость некоторых элементов управления зависит от наличия элементов в
                // списке рекомендаций, а они могут загружаться в фоне
                adapter.registerAdapterDataObserver(emptyListObserver);

                // Initial page size to fetch can also be configured here too
                final PagedList.Config config = new PagedList.Config.Builder().setPageSize(20).build();

                final List<Long> plIds = VideoDatabase.getDbInstance(WatchVideoActivity.this).playlistInfoDao().getAllIds();

                final DataSource.Factory factory =
                        new VideoItemMultPlaylistsOnlyNewOnlineDataSourceFactory(
                                WatchVideoActivity.this, plIds, false, null);

                videoItemsLiveData = new LivePagedListBuilder(factory, config).build();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        videoItemsLiveData.observe(WatchVideoActivity.this, new Observer<PagedList<VideoItem>>() {
                            @Override
                            public void onChanged(@Nullable PagedList<VideoItem> videos) {
                                adapter.submitList(videos);
                            }
                        });

                        videoList.setAdapter(adapter);
                    }
                });
            }
        }).start();
    }
}
