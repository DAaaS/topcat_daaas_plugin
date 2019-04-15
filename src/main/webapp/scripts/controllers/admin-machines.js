
(function() {
    'use strict';

    var app = angular.module('topcat');

    app.controller('AdminMachinesController', function($scope, $state, $uibModal, $q){
    	var that = this;
      this.facilities = tc.adminFacilities();

      if(!$state.params.facilityName){
        $state.go('admin.machines', {facilityName: this.facilities[0].config().name});
        return;
      }

      var facility = tc.facility($state.params.facilityName);
      var admin = facility.admin();
      var daaas = admin.daaas();
      var timeout = $q.defer();
      $scope.$on('$destroy', function(){ timeout.resolve(); });

      this.state = 'ACQUIRED';
      this.host = '';
      this.machines = [];

      this.update = function(){
        daaas.machines(timeout.promise, [
          "where 1 = 1",
          function(){
            if(that.host != ''){
              return ["and machine.host like concat('%', ?, '%') ", that.host];
            }
          }
        ]).then(function(machines){
          that.machines = machines;

          _.each(machines, function(machine){
              _.each(machine.users,  function(user){

                  if(user.userName == facility.icat().session().username && user.type == 'SECONDARY'){
                    machine.type = user.type;
                  }

                  if(user.type == 'PRIMARY'){
                      facility.icat().query(timeout.promise, [
                          "select user from User user",
                          "where user.name = ?", user.userName
                      ]).then(function(users){
                          if(users[0]){
                              machine.primaryUser = users[0];
                          }
                      });
                  }
              });
          });
        });
      };

      this.update();

    });

})();