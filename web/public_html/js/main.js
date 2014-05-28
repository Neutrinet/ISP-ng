// disclaimer: this script is written by someone who's not particularly good
// or interested in javascript. anyone willing to port this to something decent
// like angular.js, please do so

function VPN() {
    var vpn = this;
    this.endpoint = 'https://localhost:8080/';
    this.registration = {};
    //this.endpoint = '/';

    this.createUser = function(username) {

    };

    this.handleIfError = function(response) {
        if (response === undefined) {
            app.content.empty();
            app.content.append($('<h2>').text('An error ocurred'));
            app.content.append($('<p>').text('Our backend did not return a response. Please try again.'));
            app.preloader.hide();
            app.content.fadeIn();

            return true;
        }

        if (response.errorKey !== undefined) {
            app.content.empty();
            app.content.append($('<h2>').text('An error ocurred'));
            app.content.append($('<p>').text(response.message));
            app.content.append($('<p>').text(response.errorKey));
            app.preloader.hide();
            app.content.fadeIn();

            return true;
        }

        return false;
    };

    this.validateKey = function(email, key) {
        app.content.hide();
        app.preloader.show();
        $.ajax(vpn.endpoint + 'api/reg/validateKey', {
            data: JSON.stringify({'email': email, 'key': key}),
            type: 'POST',
            contentType: 'application/json',
            dataType: 'json',
            success: function(response, status, xhr) {
                if (vpn.handleIfError(response))
                    return;
                vpn.registration = response;
                app.preloader.hide();
                app.content.load('csr.html', function() {
                    app.content.fadeIn();
                    $('#use-eid').click(function() {
                        app.useEIDIdentification();
                    });
                });
            }});
    };
}

function App() {
    var self = this;

    this.preloader = $('#preloader');
    this.content = $('#content');
    this.token = "";
    this.vpn = new VPN();
    this.urlParams = {};

    this.run = function() {

        //setup
        $.ajaxSetup({
            error: self.ajaxError
        });

        window.onpopstate = self.parseQueryString;

        //activate starz
        var mq = window.matchMedia('only screen and (min-device-width: 800px)');
        if (mq.matches) {
            $.getScript('js/starz.js', function() {
                new StarField('starz').render(50, 1);
            });
        }

        //load content
        self.content.hide();

        //check for re-entry
        self.parseQueryString();

        if (self.urlParams["flow"] != undefined) {
            self.handleFlow();
            return;
        }

        $('#content').load('start.html', function() {
            self.preloader.fadeOut('slow');
            self.content.fadeIn();
            $('form .btn-primary').click(function(event) {
                self.vpn.validateKey($('#signup-email').val(), $('#signup-key').val());
            });
        });
    };

    this.handleFlow = function() {
        self.vpn.registration.id = self.urlParams["id"];

        if (self.vpn.registration.id == undefined)
            return;

        if (self[self.urlParams["flow"]] == undefined) {
            self.ajaxError(null, null, "Illegal flow");
        } else {
            // execute flow handler
            self[self.urlParams["flow"]]();
        }
    };

    this.eIdDone = function() {
        $.ajax(self.vpn.endpoint + 'api/reg/' + self.vpn.registration.id, {
            type: 'GET',
            contentType: 'application/json',
            dataType: 'json',
            success: function(response, status, xhr) {
                $('#content').load('review.html', function() {
                    $.getScript('js/renderjson.js', function() {
                        app.preloader.hide();
                        $('#ip-address input[type="checkbox"]').bootstrapSwitch();
                        $('#user-details').append(renderjson(response.user));
                        app.content.fadeIn();
                    });
                });
            }});
    };

    this.parseQueryString = function() {
        var match,
                pl = /\+/g, // Regex for replacing addition symbol with a space
                search = /([^&=]+)=?([^&]*)/g,
                decode = function(s) {
                    return decodeURIComponent(s.replace(pl, " "));
                },
                query = window.location.search.substring(1);

        self.urlParams = {};
        while (match = search.exec(query))
            self.urlParams[decode(match[1])] = decode(match[2]);
    };

    this.ajaxError = function(xhr, errorType, exception) {
        var errorMessage = exception || xhr.statusText;

        app.content.html('<h2>Error</h2><p>' + errorMessage + '</p>');
        app.preloader.hide();
        app.content.fadeIn();
    };

    this.useEIDIdentification = function() {
        app.content.hide();
        app.preloader.show();
        app.content.load('eid.html', function() {
            $.getScript('js/deployJava.js', function() {
                // ugly hack to allow vanilla deployJava.js version
                // http://stackoverflow.com/questions/13517790/using-deployjava-runapplet-to-target-specific-element
                function docWriteWrapper(jq, func) {
                    var oldwrite = document.write, content = '';
                    document.write = function(text) {
                        content += text;
                    };
                    func();
                    document.write = oldwrite;
                    jq.html(content);
                }

                var attributes = {
                    alt: 'eID applet',
                    code: 'be.fedict.eid.applet.Applet.class',
                    archive: 'objects/eid-applet-package-1.2.0-SNAPSHOT.jar',
                    width: 600,
                    height: 300
                };
                var parameters = {
                    TargetPage: app.vpn.endpoint + 'flow?id=' + self.vpn.registration.id,
                    AppletService: app.vpn.endpoint + 'applet',
                    BackgroundColor: '#ffffff',
                    codebase_lookup: 'false',
                    cache_option: 'no'
                };
                var version = '1.6';

                // fix for Mac OS X 64 bit
                var javaArgs = '';
                if (navigator.userAgent.indexOf('Mac OS X 10_6') !== -1
                        || navigator.userAgent.indexOf('Mac OS X 10.6') !== -1) {
                    javaArgs += '-d32';
                }
                parameters.java_arguments = javaArgs;
                // fix for IE 7/8
                var browser = deployJava.getBrowser();
                if (browser === 'MSIE') {
                    version = '1.6.0_27';
                }

                docWriteWrapper($('#applet-container'), function() {
                    deployJava.runApplet(attributes, parameters, version);
                });
                app.preloader.hide();
                app.content.fadeIn();
            });
        });
    };
}

var app = new App();
app.run();