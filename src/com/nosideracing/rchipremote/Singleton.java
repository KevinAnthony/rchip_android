package com.nosideracing.rchipremote;

import android.app.Application;

public class Singleton extends Application {
	{
		super.onCreate();
		initSingletons();
	}

	protected void initSingletons() {
		JSON.initInstance();
	}

}
