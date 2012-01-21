VERSION=`sed -n '/ *android:versionName="/{;s///;s/".*$$//;p;q;}' AndroidManifest.xml`
PACKAGE=rchip_remote
ARCHIVE=$(PACKAGE)-$(VERSION).tar.gz
APK=rchip_android.apk
DIST_FILES=README INSTALL TODO COPYING $(APK)
dist:
	cp bin/$(APK) $(APK)
	tar -zcvf $(ARCHIVE) $(DIST_FILES)
	rm $(APK)
