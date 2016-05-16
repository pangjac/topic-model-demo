/* SET RANDOM LOADER COLORS FOR DEMO PURPOSES */	
	setSkin('blue');
	

	/* RANDOM LARGE IMAGES FOR DEMO PURPOSES */	
	//var demoImgArray = ['http://www.hdwallpapers.in/walls/halloween_2013-wide.jpg', 'http://www.hdwallpapers.in/walls/2013_print_tech_lamborghini_aventador-wide.jpg', 'http://www.hdwallpapers.in/walls/ama_dablam_himalaya_mountains-wide.jpg', 'http://www.hdwallpapers.in/walls/arrow_tv_series-wide.jpg', 'http://www.hdwallpapers.in/walls/anna_in_frozen-wide.jpg', 'http://www.hdwallpapers.in/walls/frozen_elsa-wide.jpg', 'http://www.hdwallpapers.in/walls/shraddha_kapoor-wide.jpg', 'http://www.hdwallpapers.in/walls/sahara_force_india_f1_team-HD.jpg', 'http://www.hdwallpapers.in/walls/lake_sunset-wide.jpg', 'http://www.hdwallpapers.in/walls/2013_movie_cloudy_with_a_chance_of_meatballs_2-wide.jpg', 'http://www.hdwallpapers.in/walls/bates_motel_2013_tv_series-wide.jpg', 'http://www.hdwallpapers.in/walls/krrish_3_movie-wide.jpg', 'http://www.hdwallpapers.in/walls/universe_door-wide.jpg', 'http://www.hdwallpapers.in/walls/night_rider-HD.jpg', 'http://www.hdwallpapers.in/walls/tide_and_waves-wide.jpg', 'http://www.hdwallpapers.in/walls/2014_lamborghini_veneno_roadster-wide.jpg', 'http://www.hdwallpapers.in/walls/peeta_katniss_the_hunger_games_catching_fire-wide.jpg', 'http://www.hdwallpapers.in/walls/captain_america_the_winter_soldier-wide.jpg', 'http://www.hdwallpapers.in/walls/puppeteer_ps3_game-wide.jpg', 'http://www.hdwallpapers.in/walls/lunar_space_galaxy-HD.jpg', 'http://www.hdwallpapers.in/walls/2013_wheelsandmore_lamborghini_aventador-wide.jpg', 'http://www.hdwallpapers.in/walls/destiny_2014_game-wide.jpg', 'http://www.hdwallpapers.in/colors_of_nature-wallpapers.html', 'http://www.hdwallpapers.in/walls/sunset_at_laguna_beach-wide.jpg'];
	
	var calcPercent;
	
	$progress = $('.progress-bar');
	$percent = $('.percentage');
	
	total_time = 15*60*1000
	// Call function to load array of images
	
	/* WHEN LOADED */
	$('#google-signin').click(function() {
		
		var request = $.ajax({
			method: "GET",
			url: "http://127.0.0.1:8000/run"
		}).done(function(data) {
    		if ( console && console.log ) {
      			console.log( "Sample of data:", data);
    		}

		    d3.json('mgdata/39.json', function(data) {

		    if (data != null && data != undefined) {
		    data = MG.convert.date(data, 'date');
		    MG.data_graphic({
		        title: "Topic 1 ",
		        description: "This is a simple line chart. You can remove the area portion by adding area: false to the arguments list.",
		        data: data,
		        width: 600,
		        height: 200,
		        right: 40,
		        target: document.getElementById('fake_users1'),
		        x_accessor: 'date',
		        y_accessor: 'value'
		    });
		}
		});

		    d3.json('mgdata/sample_48.json', function(data) {

		    if (data != null && data != undefined) {

		    data = MG.convert.date(data, 'date');
		    MG.data_graphic({
		        title: "Topic 2",
		        description: "This is a simple line chart. You can remove the area portion by adding area: false to the arguments list.",
		        data: data,
		        width: 600,
		        height: 200,
		        right: 40,
		        target: document.getElementById('fake_users2'),
		        x_accessor: 'date',
		        y_accessor: 'value'
		    });
		}
		});

		    d3.json('mgdata/sample_49.json', function(data) {

		    if (data != null && data != undefined) {

		    data = MG.convert.date(data, 'date');
		    MG.data_graphic({
		        title: "Topic 3",
		        description: "This is a simple line chart. You can remove the area portion by adding area: false to the arguments list.",
		        data: data,
		        width: 600,
		        height: 200,
		        right: 40,
		        target: document.getElementById('fake_users3'),
		        x_accessor: 'date',
		        y_accessor: 'value'
		    });
		}
		});


    	});
	
		$('#bar-status').text('Running...').removeClass('loaded');
		$percent.text('0%');
		$progress.css('width', "0%");
		calcPercent = setInterval(function() {
			$percent.text(Math.floor(($progress.width() / $('.loader').width()) * 100) + '%');
		},100);

		$progress.animate({
			width: "100%"
			}, total_time, function() {
			clearInterval(calcPercent);
			$('#bar-status').text('Finished!').addClass('loaded');
			$percent.text('100%');
		})
	});
	
	function setSkin(skin){
		$('.loader').attr('class', 'loader '+skin);
		$('span').hasClass('loaded') ? $('span').attr('class', 'loaded '+skin) : $('span').attr('class', skin);
	}