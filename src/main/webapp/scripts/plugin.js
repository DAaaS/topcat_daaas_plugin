

registerTopcatPlugin(function(pluginUrl){
	return {
		scripts: [
			pluginUrl + 'scripts/controllers/create-machine.js',
			pluginUrl + 'scripts/controllers/my-machines.js',
			pluginUrl + 'scripts/controllers/admin-machines.js',
			pluginUrl + 'scripts/controllers/share-machine.js',
			pluginUrl + 'scripts/directives/file-upload.js',

			pluginUrl + 'scripts/services/tc-admin-daaas.js',
			pluginUrl + 'scripts/services/tc-user-daaas.js'
		],

		stylesheets: [
			pluginUrl + 'styles/main.css'
		],

		configSchema: {
			daaas: {
				createMachineDelaySeconds: {_type: "number"} 
			}
		},

		extend: {
			admin: function(tcAdminDaaas){
				var daaas;

				this.daaas = function(){
					if(!daaas) daaas = tcAdminDaaas.create(pluginUrl, this);
					return daaas;
				};
			},

			user: function(tcUserDaaas){
				var daaas;

				this.daaas = function(){
					if(!daaas) daaas = tcUserDaaas.create(pluginUrl, this);
					return daaas;
				};
			}
			
		},

		setup: function(tc){

			tc.ui().registerMainTab('my-machines', pluginUrl + 'views/my-machines.html', {
				insertAfter: 'my-data',
				controller: 'MyMachinesController as myMachinesController',
				multiFacility: true
			});

			tc.ui().registerAdminTab('machines', pluginUrl + 'views/admin-machines.html', {
				insertAfter: 'downloads',
				controller: 'AdminMachinesController as adminMachinesController',
				multiFacility: true
			});

		},

		login: function(){
			//'this' is the facility
			//potentially register tabs etc
			//can return promise
		},

		logout: function(){
			//'this' is the facility
			//potentially un register tabs etc
			//can return promise
		}

	};
});

