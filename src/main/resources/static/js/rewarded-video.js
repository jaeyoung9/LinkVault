/**
 * LinkVault Rewarded Video - Google IMA SDK Integration
 * Loads IMA SDK on demand, plays a rewarded video ad, and awards points on completion.
 */
(function() {
    'use strict';

    var imaLoaded = false;
    var imaLoading = false;
    var pendingCallback = null;

    /**
     * Dynamically load the Google IMA SDK script.
     */
    function loadImaSdk(callback) {
        if (imaLoaded && window.google && window.google.ima) {
            callback();
            return;
        }
        if (imaLoading) {
            pendingCallback = callback;
            return;
        }
        imaLoading = true;
        var script = document.createElement('script');
        script.src = 'https://imasdk.googleapis.com/js/sdkloader/ima3.js';
        script.async = true;
        script.onload = function() {
            imaLoaded = true;
            imaLoading = false;
            callback();
            if (pendingCallback) {
                pendingCallback();
                pendingCallback = null;
            }
        };
        script.onerror = function() {
            imaLoading = false;
            if (typeof showToast === 'function') {
                showToast('Failed to load video ad SDK. Please disable ad blockers and try again.', 'error');
            }
        };
        document.head.appendChild(script);
    }

    /**
     * Create and show the video player modal.
     */
    function createVideoModal() {
        var existing = document.getElementById('rewardVideoModal');
        if (existing) existing.remove();

        var modal = document.createElement('div');
        modal.id = 'rewardVideoModal';
        modal.className = 'reward-video-modal';
        modal.innerHTML =
            '<div class="reward-video-container">' +
            '  <div id="imaAdContainer" style="position:absolute;top:0;left:0;width:100%;height:100%;"></div>' +
            '  <video id="imaContentVideo" style="width:100%;height:100%;background:#000;" playsinline></video>' +
            '  <div id="imaLoadingOverlay" style="position:absolute;top:0;left:0;width:100%;height:100%;display:flex;align-items:center;justify-content:center;color:#fff;font-size:1rem;">' +
            '    Loading video ad...' +
            '  </div>' +
            '</div>';

        document.body.appendChild(modal);
        return modal;
    }

    /**
     * Play a rewarded video ad using IMA SDK.
     */
    function playRewardedVideo(adTagUrl, nonce, pointsPerVideo) {
        var modal = createVideoModal();
        var adContainer = document.getElementById('imaAdContainer');
        var contentVideo = document.getElementById('imaContentVideo');
        var loadingOverlay = document.getElementById('imaLoadingOverlay');

        var adDisplayContainer;
        var adsLoader;
        var adsManager;
        var completed = false;

        function cleanup() {
            if (adsManager) {
                try { adsManager.destroy(); } catch(e) {}
            }
            if (adsLoader) {
                try { adsLoader.destroy(); } catch(e) {}
            }
            if (adDisplayContainer) {
                try { adDisplayContainer.destroy(); } catch(e) {}
            }
            modal.remove();
        }

        try {
            // Initialize IMA
            adDisplayContainer = new google.ima.AdDisplayContainer(adContainer, contentVideo);
            adDisplayContainer.initialize();

            adsLoader = new google.ima.AdsLoader(adDisplayContainer);

            adsLoader.addEventListener(
                google.ima.AdsManagerLoadedEvent.Type.ADS_MANAGER_LOADED,
                function(event) {
                    loadingOverlay.style.display = 'none';

                    var renderingSettings = new google.ima.AdsRenderingSettings();
                    renderingSettings.restoreCustomPlaybackStateOnAdBreakComplete = true;

                    adsManager = event.getAdsManager(contentVideo, renderingSettings);

                    adsManager.addEventListener(google.ima.AdEvent.Type.COMPLETE, function() {
                        completed = true;
                        cleanup();
                        completeRewardVideo(nonce);
                    });

                    adsManager.addEventListener(google.ima.AdEvent.Type.ALL_ADS_COMPLETED, function() {
                        if (!completed) {
                            completed = true;
                            cleanup();
                            completeRewardVideo(nonce);
                        }
                    });

                    adsManager.addEventListener(google.ima.AdErrorEvent.Type.AD_ERROR, function(adError) {
                        cleanup();
                        if (typeof showToast === 'function') {
                            showToast('Ad playback error. Please try again.', 'error');
                        }
                    });

                    adsManager.addEventListener(google.ima.AdEvent.Type.SKIPPED, function() {
                        cleanup();
                        if (typeof showToast === 'function') {
                            showToast('Video skipped. No points awarded.', 'error');
                        }
                    });

                    try {
                        var width = adContainer.offsetWidth;
                        var height = adContainer.offsetHeight;
                        adsManager.init(width, height, google.ima.ViewMode.NORMAL);
                        adsManager.start();
                    } catch (initError) {
                        cleanup();
                        if (typeof showToast === 'function') {
                            showToast('Error starting video ad.', 'error');
                        }
                    }
                },
                false
            );

            adsLoader.addEventListener(
                google.ima.AdErrorEvent.Type.AD_ERROR,
                function(event) {
                    cleanup();
                    if (typeof showToast === 'function') {
                        showToast('Failed to load video ad. Please try again later.', 'error');
                    }
                },
                false
            );

            // Request ads
            var adsRequest = new google.ima.AdsRequest();
            adsRequest.adTagUrl = adTagUrl;
            adsRequest.linearAdSlotWidth = adContainer.offsetWidth || 640;
            adsRequest.linearAdSlotHeight = adContainer.offsetHeight || 360;
            adsRequest.nonLinearAdSlotWidth = adContainer.offsetWidth || 640;
            adsRequest.nonLinearAdSlotHeight = Math.floor((adContainer.offsetHeight || 360) / 3);

            adsLoader.requestAds(adsRequest);

        } catch (err) {
            cleanup();
            if (typeof showToast === 'function') {
                showToast('Error initializing video player.', 'error');
            }
        }

        // Handle window resize
        window.addEventListener('resize', function onResize() {
            if (adsManager && document.getElementById('rewardVideoModal')) {
                var w = adContainer.offsetWidth;
                var h = adContainer.offsetHeight;
                adsManager.resize(w, h, google.ima.ViewMode.NORMAL);
            } else {
                window.removeEventListener('resize', onResize);
            }
        });
    }

    /**
     * Entry point: called from the "Watch a Video" button.
     * Requests a nonce from the server, loads IMA SDK, and plays the ad.
     */
    window.requestRewardVideo = function() {
        var btn = document.querySelector('.reward-video-btn');
        if (btn) { btn.disabled = true; btn.textContent = 'Loading...'; }

        fetch('/api/reward/request-video', {
            method: 'POST',
            headers: csrfHeaders({ 'Content-Type': 'application/json' })
        })
        .then(function(r) {
            if (!r.ok) return r.json().then(function(err) { throw err; });
            return r.json();
        })
        .then(function(data) {
            if (data.error) {
                showToast(data.error, 'error');
                resetBtn(btn);
                return;
            }

            if (!data.adTagUrl || data.adTagUrl.trim() === '') {
                showToast('Rewarded video is not configured yet. Please try again later.', 'error');
                resetBtn(btn);
                return;
            }

            showToast('Watch the video to earn ' + (data.pointsPerVideo || 10) + ' points!');

            loadImaSdk(function() {
                resetBtn(btn);
                playRewardedVideo(data.adTagUrl, data.nonce, data.pointsPerVideo);
            });
        })
        .catch(function(err) {
            showToast(err.message || 'Error requesting video', 'error');
            resetBtn(btn);
        });
    };

    function resetBtn(btn) {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '&#127916; Watch a Video';
        }
    }

})();
