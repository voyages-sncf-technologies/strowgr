package com.vsct.dt.strowgr.admin.core.rx;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public interface EventObserver<R> extends SingleObserver<R> {

    @Override
    default void onSubscribe(Disposable disposable) {
        // no-op by default
    }

}
