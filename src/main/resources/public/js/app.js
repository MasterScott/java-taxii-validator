/*
 * Copyright (c) 2015, The MITRE Corporation. All rights reserved.
 * See LICENSE for complete terms.
 *
 * Java-TAXII-Valdiator
 *
 * nemonik (Michael Joseph Walsh <github.com@nemonik.com>)
 */

var app = angular.module('validatorapp', [ 'ui.bootstrap', 'ngRoute',
		'ngFileUpload' ]);

app.config(function($routeProvider) {
	$routeProvider.when('/', {
		templateUrl : 'views/main.html',
		controller : 'AppController'
	}).otherwise({
		redirectTo : '/'
	})
});

// app
// .controller(
// 'RcvController',
// function($scope, $http, $location, Upload) {

app
		.controller(
				'AppController',
				[
						'$scope',
						'$http',
						'$location',
						'Upload',
						function($scope, $http, $location, Upload) {

							$scope.validateURL = function(value) {
								return /^(https?|ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i
										.test(value);
							}

							$scope.onTabSelect = function(tab) {
								$scope.selectedTab = tab;
								console.log("Changed tab to " + tab);
							}

							$scope.closeAlert = function(tab) {
								$scope.taxii[tab].alert = null;
							};

							$scope.reset = function(tab) {
								if (typeof tab === 'undefined') {
									tab = $scope.selectedTab
									console.log("called reset() on tab " + tab);
								} else {
									console.log("called reset() with " + tab);
								}
								$scope.taxii[tab] = {
									results : {
										fail : {
											isCollapsed : true
										},
										success : {
											isCollapsed : true
										}
									}
								};
							};

							$scope.taxii = {};
							$scope.reset('fileTab');
							$scope.reset('xmlTab');
							$scope.reset('urlTab');
							$scope.selectedTab = 'urlTab';

							$scope.validate = function(tab, method) {
								console.log(method + ' = '
										+ $scope.taxii[tab][method]);

								if (!$scope.taxii[tab][method]
										|| $scope.taxii[tab][method] === "") {
									if (method === "url") {
										$scope.taxii[tab].alert = {
											type : 'danger',
											msg : 'No URL entered.'
										};
									} else {
										$scope.taxii[tab].alert = {
											type : 'danger',
											msg : 'No XML string entered.'
										};
									}
									return;
								} else if ((method === "url")
										&& (!$scope.validateURL($scope.taxii[tab][method]))) {
									$scope.taxii[tab].alert = {
										type : 'danger',
										msg : 'Not a valid URL.'
									};
									return;
								} else {
									$scope.closeAlert(tab);
								}

								$http({
									method : 'POST',
									url : '/api/v1/validate/' + method,
									data : $scope.taxii[tab][method]
								})
										.success(
												function(data, status, headers,
														config) {
													console.log("Success");
													if (data.validates === "true") {
														$scope.taxii[tab].results.success = {
															msg : "Passes validation.",
															data : data,
															isCollapsed : false
														};
													} else {
														$scope.taxii[tab].results.fail = {
															msg : "Fails validation.",
															data : data,
															isCollapsed : false
														};
													}
												}).error(
												function(data, status, headers,
														config) {
													console.log('Error: '
															+ data);
												});
							}

							$scope.$watch('files', function() {
								$scope.upload($scope.files);
							});

							$scope.upload = function(files) {
								if (files && files.length) {
									for (var i = 0; i < files.length; i++) {
										console.log(files[i]);
										var file = files[i];
										Upload.upload({
											url : '/api/v1/validate/file',
											file : file
										}).progress(function(evt) {
											var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
											console.log('progress: ' + progressPercentage + '% ' + evt.config.file.name);
										}).success(function(data, status, headers, config) {
											console.log('file ' + config.file.name + 'uploaded. Response: ' + data);
										});
									}
								}
							};
						} ]);
