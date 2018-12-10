package com.kunfei.bookshelf.model.content;

import com.kunfei.basemvplib.BaseModelImpl;
import com.kunfei.bookshelf.bean.BaseChapterBean;
import com.kunfei.bookshelf.model.BookSourceManager;
import com.kunfei.bookshelf.model.impl.IHttpGetApi;
import com.kunfei.bookshelf.model.impl.IHttpPostApi;
import com.kunfei.bookshelf.model.impl.IStationBookModel;
import com.kunfei.bookshelf.MApplication;
import com.kunfei.bookshelf.bean.BaseChapterBean;
import com.kunfei.bookshelf.bean.BookContentBean;
import com.kunfei.bookshelf.bean.BookShelfBean;
import com.kunfei.bookshelf.bean.BookSourceBean;
import com.kunfei.bookshelf.bean.ChapterListBean;
import com.kunfei.bookshelf.bean.SearchBookBean;
import com.kunfei.bookshelf.model.BookSourceManager;
import com.kunfei.bookshelf.model.analyzeRule.AnalyzeHeaders;
import com.kunfei.bookshelf.model.analyzeRule.AnalyzeSearchUrl;
import com.kunfei.bookshelf.model.impl.IHttpGetApi;
import com.kunfei.bookshelf.model.impl.IHttpPostApi;
import com.kunfei.bookshelf.model.impl.IStationBookModel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static android.text.TextUtils.isEmpty;

/**
 * 默认检索规则
 */
public class DefaultModel extends BaseModelImpl implements IStationBookModel {
    private String tag;
    private String name;
    private BookSourceBean bookSourceBean;
    private Map<String, String> headerMap = AnalyzeHeaders.getMap(null);

    private DefaultModel(String tag) {
        this.tag = tag;
        try {
            URL url = new URL(tag);
            name = url.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            name = tag;
        }
    }

    public static DefaultModel getInstance(String tag) {
        return new DefaultModel(tag);
    }

    private Boolean initBookSourceBean() {
        if (bookSourceBean == null) {
            BookSourceBean sourceBean = BookSourceManager.getBookSourceByUrl(tag);
            if (sourceBean != null) {
                bookSourceBean = sourceBean;
                name = bookSourceBean.getBookSourceName();
                headerMap = AnalyzeHeaders.getMap(bookSourceBean.getHttpUserAgent());
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * 发现
     */
    @Override
    public Observable<List<SearchBookBean>> findBook(String url, int page) {
        if (!initBookSourceBean() || isEmpty(bookSourceBean.getRuleSearchUrl())) {
            return Observable.create(emitter -> {
                emitter.onNext(new ArrayList<>());
                emitter.onComplete();
            });
        }
        BookList bookList = new BookList(tag, name, bookSourceBean);
        try {
            AnalyzeSearchUrl analyzeSearchUrl = new AnalyzeSearchUrl(url, "", page);
            if (analyzeSearchUrl.getSearchUrl() == null) {
                return Observable.create(emitter -> {
                    emitter.onNext(new ArrayList<>());
                    emitter.onComplete();
                });
            }
            if (url.contains("@")) {
                return getRetrofitString(analyzeSearchUrl.getSearchUrl())
                        .create(IHttpPostApi.class)
                        .searchBook(analyzeSearchUrl.getSearchPath(),
                                analyzeSearchUrl.getQueryMap(),
                                headerMap)
                        .flatMap(bookList::analyzeSearchBook);
            } else if (url.contains("?")) {
                return getRetrofitString(analyzeSearchUrl.getSearchUrl())
                        .create(IHttpGetApi.class)
                        .searchBook(analyzeSearchUrl.getSearchPath(),
                                analyzeSearchUrl.getQueryMap(),
                                headerMap)
                        .flatMap(bookList::analyzeSearchBook);
            } else {
                return getRetrofitString(analyzeSearchUrl.getSearchUrl())
                        .create(IHttpGetApi.class)
                        .getWebContent(analyzeSearchUrl.getSearchPath(),
                                headerMap)
                        .flatMap(bookList::analyzeSearchBook);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Observable.create(emitter -> {
                emitter.onNext(new ArrayList<>());
                emitter.onComplete();
            });
        }
    }

    /**
     * 搜索
     */
    @Override
    public Observable<List<SearchBookBean>> searchBook(String content, int page) {
        if (!initBookSourceBean() || isEmpty(bookSourceBean.getRuleSearchUrl())) {
            return Observable.create(emitter -> {
                emitter.onNext(new ArrayList<>());
                emitter.onComplete();
            });
        }
        BookList bookList = new BookList(tag, name, bookSourceBean);
        try {
            AnalyzeSearchUrl analyzeSearchUrl = new AnalyzeSearchUrl(bookSourceBean.getRuleSearchUrl(), content, page);
            if (analyzeSearchUrl.getSearchUrl() == null) {
                return Observable.create(emitter -> {
                    emitter.onNext(new ArrayList<>());
                    emitter.onComplete();
                });
            }
            if (bookSourceBean.getRuleSearchUrl().contains("@")) {
                return getRetrofitString(analyzeSearchUrl.getSearchUrl())
                        .create(IHttpPostApi.class)
                        .searchBook(analyzeSearchUrl.getSearchPath(),
                                analyzeSearchUrl.getQueryMap(),
                                headerMap)
                        .flatMap(bookList::analyzeSearchBook);
            } else if (bookSourceBean.getRuleSearchUrl().contains("?")) {
                return getRetrofitString(analyzeSearchUrl.getSearchUrl())
                        .create(IHttpGetApi.class)
                        .searchBook(analyzeSearchUrl.getSearchPath(),
                                analyzeSearchUrl.getQueryMap(),
                                headerMap)
                        .flatMap(bookList::analyzeSearchBook);
            } else {
                return getRetrofitString(analyzeSearchUrl.getSearchUrl())
                        .create(IHttpGetApi.class)
                        .getWebContent(analyzeSearchUrl.getSearchPath(),
                                headerMap)
                        .flatMap(bookList::analyzeSearchBook);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Observable.create(emitter -> {
                emitter.onNext(new ArrayList<>());
                emitter.onComplete();
            });
        }
    }

    /**
     * 获取书籍信息
     */
    @Override
    public Observable<BookShelfBean> getBookInfo(final BookShelfBean bookShelfBean) {
        if (!initBookSourceBean()) {
            return Observable.error(new Throwable(String.format("无法找到源%s", tag)));
        }
        BookInfo bookInfo = new BookInfo(tag, name, bookSourceBean);
        return getRetrofitString(tag)
                .create(IHttpGetApi.class)
                .getWebContent(bookShelfBean.getNoteUrl(), headerMap)
                .flatMap(response -> bookInfo.analyzeBookInfo(response.body(), bookShelfBean));
    }

    /**
     * 获取目录
     */
    @Override
    public Observable<List<ChapterListBean>> getChapterList(final BookShelfBean bookShelfBean) {
        if (!initBookSourceBean()) {
            return Observable.create(emitter -> {
                emitter.onError(new Throwable(String.format("%s没有找到书源配置", bookShelfBean.getBookInfoBean().getName())));
                emitter.onComplete();
            });
        }
        BookChapter bookChapter = new BookChapter(tag, bookSourceBean);
        return getRetrofitString(tag)
                .create(IHttpGetApi.class)
                .getWebContent(bookShelfBean.getBookInfoBean().getChapterUrl(), headerMap)
                .flatMap(response -> bookChapter.analyzeChapterList(response.body(), bookShelfBean));
    }

    /**
     * 获取正文
     */
    @Override
    public Observable<BookContentBean> getBookContent(final Scheduler scheduler, final BaseChapterBean chapterBean) {
        if (!initBookSourceBean()) {
            return Observable.create(emitter -> {
                emitter.onNext(new BookContentBean());
                emitter.onComplete();
            });
        }
        BookContent bookContent = new BookContent(tag, bookSourceBean);
        if (bookSourceBean.getRuleBookContent().startsWith("$")) {
            return getAjaxHtml(MApplication.getInstance(), chapterBean.getDurChapterUrl(), AnalyzeHeaders.getUserAgent(bookSourceBean.getHttpUserAgent()))
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(scheduler)
                    .flatMap(response -> bookContent.analyzeBookContent(response, chapterBean));
        } else {
            return getRetrofitString(tag)
                    .create(IHttpGetApi.class)
                    .getWebContent(chapterBean.getDurChapterUrl(), headerMap)
                    .subscribeOn(scheduler)
                    .flatMap(response -> bookContent.analyzeBookContent(response.body(), chapterBean));
        }
    }

}