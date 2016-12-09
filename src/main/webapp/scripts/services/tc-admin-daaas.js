'use strict';

(function(){
 
  var app = angular.module('topcat');

  app.service('tcAdminDaaas', function($sessionStorage, $rootScope, $timeout, helpers){
  	
    this.create = function(pluginUrl, admin){
      return new AdminDaaas(pluginUrl, admin);
    };

    function AdminDaaas(pluginUrl, admin){

      var facility = admin.facility();
      var icat = facility.icat();

      this.pluginUrl = function(){
        return pluginUrl;
      };

      this.images = helpers.overload({
        'object': function(options){
          return this.get('images', {sessionId: icat.session().sessionId}, options);
        },
        'promise': function(timeout){
          return this.images({timeout: timeout});
        },
        '': function(){
          return this.images({});
        }
      });

    	this.flavors = helpers.overload({
    		'object': function(options){
    			return this.get('flavors', {sessionId: icat.session().sessionId}, options);
    		},
    		'promise': function(timeout){
    			return this.flavors({timeout: timeout});
    		},
    		'': function(){
    			return this.flavors({});
    		}
    	});

      this.availabilityZones = helpers.overload({
        'object': function(options){
          return this.get('availabilityZones', {sessionId: icat.session().sessionId}, options);
        },
        'promise': function(timeout){
          return this.availabilityZones({timeout: timeout});
        },
        '': function(){
          return this.availabilityZones({});
        }
      });

      this.machineTypes = helpers.overload({
        'object': function(options){
          return this.get('machineTypes', {sessionId: icat.session().sessionId}, options);
        },
        'promise': function(timeout){
          return this.machineTypes({timeout: timeout});
        },
        '': function(){
          return this.machineTypes({});
        }
      });

      this.createMachineType = helpers.overload({
        'string, string, string, string, number, string, array, object': function(name, imageId, flavorId, availabilityZone, poolSize, personality, scopes, options){
          return this.post('machineTypes', {
            json: JSON.stringify({
              sessionId: icat.session().sessionId,
              name: name,
              imageId: imageId,
              flavorId: flavorId,
              availabilityZone: availabilityZone,
              poolSize: poolSize,
              personality: personality,
              scopes: scopes
            })
          }, options);
        },
        'promise, string, string, string, string, number, string, array': function(timeout, name, imageId, flavorId, availabilityZone, poolSize, personality, scopes){
          return this.createMachineType(name, imageId, flavorId, availabilityZone, poolSize, personality, scopes, {timeout: timeout});
        },
        'string, string, string, string, number, string, array': function(name, imageId, flavorId, availabilityZone, poolSize, personality, scopes){
          return this.createMachineType(name, imageId, flavorId, availabilityZone, poolSize, personality, scopes, {});
        }
      });

      this.updateMachineType = helpers.overload({
        'number, string, string, string, string, number, string, array, object': function(id, name, imageId, flavorId, availabilityZone, poolSize, personality, scopes, options){
          return this.put('machineTypes/' + id, {
            json: JSON.stringify({
              sessionId: icat.session().sessionId,
              name: name,
              imageId: imageId,
              flavorId: flavorId,
              availabilityZone: availabilityZone,
              poolSize: poolSize,
              personality: personality,
              scopes: scopes
            })
          }, options);
        },
        'promise, number, string, string, string, string, number, string, array': function(timeout, id, name, imageId, flavorId, availabilityZone, poolSize, personality, scopes){
          return this.updateMachineType(id, name, imageId, flavorId, availabilityZone, poolSize, personality, scopes, {timeout: timeout});
        },
        'number, string, string, string, string, number, string, array': function(id, name, imageId, flavorId, availabilityZone, poolSize, personality, scopes){
          return this.updateMachineType(id, name, imageId, flavorId, availabilityZone, poolSize, personality, scopes, {});
        }
      });

      this.deleteMachineType = helpers.overload({
        'number, object': function(id, options){
          return this.delete('machineTypes/' + id, {sessionId: icat.session().sessionId}, options);
        },
        'promise, number': function(timeout, id){
          return this.deleteMachineType(id, {timeout: timeout});
        },
        'number': function(){
          return this.deleteMachineType(id, {});
        }
      });


      var matches;
      if(matches = pluginUrl.match(/http:\/\/localhost:10080(.*)/)){
        helpers.generateRestMethods(this, "https://localhost:8181" + matches[1] + "api/admin/");
      } else {
        helpers.generateRestMethods(this, pluginUrl + "api/admin/");
      }
    }
  });

})();