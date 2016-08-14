// implementation of AR-Experience (aka "World")
//Worldにロジックを記述する。
var World = {
	// true once data was fetched
	initiallyLoadedData: false,

	// POI-Marker asset。マーカーFishの画像
	markerDrawable_idle: null,

	// called to inject new POI data。
	// マーカーを設置する。
	loadPoisFromJsonData: function loadPoisFromJsonDataFn(poiData) {

		/*
			The example Image Recognition already explained how images are loaded and displayed in the augmented reality view. This sample loads an AR.ImageResource when the World variable was defined. It will be reused for each marker that we will create afterwards.
		*/
		//画像
		World.markerDrawable_idle = new AR.ImageResource("assets/fish.png");

		/*
			For creating the marker a new object AR.GeoObject will be created at the specified geolocation. An AR.GeoObject connects one or more AR.GeoLocations with multiple AR.Drawables. The AR.Drawables can be defined for multiple targets. A target can be the camera, the radar or a direction indicator. Both the radar and direction indicators will be covered in more detail in later examples.
		*/
		//位置情報
		var markerLocation = new AR.GeoLocation(poiData.latitude, poiData.longitude, poiData.altitude);
		//描画情報.８は８倍のサイズ
		var markerImageDrawable_idle = new AR.ImageDrawable(World.markerDrawable_idle, 8, {
			zOrder: 0,//表示順
			opacity: 1.0,//不透明
			//クリック時処理
			onClick : function() {
				//アニメーション開始
      			elevatorAnimation.start();
    		}
    	});

		//回転。アニメーションを定義(１秒間に360度回転)。click時に実行してる
    	var elevatorAnimation = new AR.PropertyAnimation(
          markerImageDrawable_idle, //the object geoLocation1 holds the animated property
          "rotation", //the property altitude will be animated
          0, //the start value of the animation
          360, //the resulting value of the animation
          1000, //the duration of the elevator climb is 10 seconds (10000 miliseconds)
          {type: AR.CONST.EASING_CURVE_TYPE.EASE_IN_OUT_QUAD}

        );


		// create GeoObject
		//AR上に配置
		var markerObject = new AR.GeoObject(markerLocation, {
			drawables: {
				cam: [markerImageDrawable_idle]
			}
		});

		// Updates status message as a user feedback that everything was loaded properly.
		//メッセージ
		World.updateStatusMessage('1 place loaded');
	},

	// updates status message shon in small "i"-button aligned bottom center
	//inde.htmlのメッセージやアイコンに状態を表示する
	updateStatusMessage: function updateStatusMessageFn(message, isWarning) {

		var themeToUse = isWarning ? "e" : "c";
		var iconToUse = isWarning ? "alert" : "info";

		$("#status-message").html(message);
		$("#popupInfoButton").buttonMarkup({
			theme: themeToUse
		});
		$("#popupInfoButton").buttonMarkup({
			icon: iconToUse
		});
	},

	//位置が変化した時の処理。メインルーチンみたいになる。
	// location updates, fired every time you call architectView.setLocation() in native environment
	locationChanged: function locationChangedFn(lat, lon, alt, acc) {

		/*
			The custom function World.onLocationChanged checks with the flag World.initiallyLoadedData if the function was already called. With the first call of World.onLocationChanged an object that contains geo information will be created which will be later used to create a marker using the World.loadPoisFromJsonData function.
		*/
		//最初だけマーカーの位置を作成する。
		if (!World.initiallyLoadedData) {
			// creates a poi object with a random location near the user's location
			var poiData = {
				"id": 1,
				"longitude": (lon + (Math.random() / 5 - 0.1)),
				"latitude": (lat + (Math.random() / 5 - 0.1)),
				"altitude": 100.0 //標高
			};
			//
			World.loadPoisFromJsonData(poiData);
			//１回だけのフラグを立てる
			World.initiallyLoadedData = true;
		}
	},
};

/* 
	Set a custom function where location changes are forwarded to. There is also a possibility to set AR.context.onLocationChanged to null. In this case the function will not be called anymore and no further location updates will be received. 
*/
//位置情報が変化した時の処理。これがメインルーチンみたいになる。
AR.context.onLocationChanged = World.locationChanged;