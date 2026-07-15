// SoundCraft Content Script for SoundCloud, Spotify, and YouTube
// Scrapes music metadata and communicates with Minecraft Client via Extension Background Script

let lastSentState = null;

function getPlatform() {
  const host = window.location.hostname;
  if (host.includes("soundcloud.com")) return "soundcloud";
  if (host.includes("spotify.com")) return "spotify";
  if (host.includes("youtube.com")) return "youtube";
  return null;
}

// Convert MM:SS or HH:MM:SS text to seconds
function parseTimeToSeconds(timeStr) {
  if (!timeStr) return 0;
  const parts = timeStr.trim().split(":").map(Number);
  if (parts.some(isNaN)) return 0;
  
  if (parts.length === 2) {
    return parts[0] * 60 + parts[1];
  } else if (parts.length === 3) {
    return parts[0] * 3600 + parts[1] * 60 + parts[2];
  }
  return 0;
}

// Extract high quality artwork URL from background-image style or elements
function cleanArtworkUrl(url) {
  if (!url) return "";
  let cleanUrl = url;
  const match = url.match(/url\("?([^"\)]+)"?\)/);
  if (match) {
    cleanUrl = match[1];
  }
  
  // Replace smaller thumbnail indicators with high-res 500x500 version
  cleanUrl = cleanUrl
    .replace("-t50x50.", "-t500x500.")
    .replace("-large.", "-t500x500.")
    .replace("-t200x200.", "-t500x500.")
    .replace("-badge.", "-t500x500.");
    
  return cleanUrl;
}

// SoundCloud Scraper
function scrapeSoundCloudState() {
  try {
    const playButton = document.querySelector(".playControl");
    if (!playButton) {
      return {
        title: "No Track Playing",
        artist: "Unknown Artist",
        artworkUrl: "",
        isPlaying: false,
        currentTime: 0,
        duration: 0
      };
    }

    const isPlaying = playButton.classList.contains("playing");

    const titleEl = document.querySelector(".playbackSoundBadge__titleLink");
    const title = titleEl ? (titleEl.title || titleEl.innerText || "").trim() : "Unknown Track";

    const artistEl = document.querySelector(".playbackSoundBadge__lightLink");
    const artist = artistEl ? (artistEl.title || artistEl.innerText || "").trim() : "Unknown Artist";

    // SoundCloud badge avatar span contains background-image
    const avatarEl = document.querySelector(".playbackSoundBadge__avatar span.image__full");
    const rawArtworkUrl = avatarEl ? getComputedStyle(avatarEl).backgroundImage : "";
    const artworkUrl = cleanArtworkUrl(rawArtworkUrl);

    const timePassedEl = document.querySelector(".playbackTimeline__timePassed span[aria-hidden='true']");
    const currentTimeText = timePassedEl ? timePassedEl.innerText : "0:00";
    const currentTime = parseTimeToSeconds(currentTimeText);

    const durationEl = document.querySelector(".playbackTimeline__duration span[aria-hidden='true']");
    const durationText = durationEl ? durationEl.innerText : "0:00";
    const duration = parseTimeToSeconds(durationText);

    return {
      title,
      artist,
      artworkUrl,
      isPlaying,
      currentTime,
      duration
    };
  } catch (e) {
    console.error("[SoundCraft] Error scraping SoundCloud state:", e);
    return null;
  }
}

// Spotify Scraper
function scrapeSpotifyState() {
  try {
    const playButton = document.querySelector('button[data-testid="control-button-playpause"]') ||
                       document.querySelector('button[data-testid="play-button"]');
    if (!playButton) {
      return {
        title: "No Track Playing",
        artist: "Unknown Artist",
        artworkUrl: "",
        isPlaying: false,
        currentTime: 0,
        duration: 0
      };
    }

    let isPlaying = false;
    const label = (playButton.getAttribute("aria-label") || "").toLowerCase();
    if (label.includes("pause") || label.includes("tạm dừng") || label.includes("dừng")) {
      isPlaying = true;
    } else {
      const path = playButton.querySelector("svg path");
      if (path) {
        const d = path.getAttribute("d") || "";
        const mCount = (d.match(/m/gi) || []).length;
        if (mCount > 1) {
          isPlaying = true;
        }
      }
    }

    const titleEl = document.querySelector('[data-testid="context-item-link"]') ||
                    document.querySelector('[data-testid="track-info-metadata-title"]') ||
                    document.querySelector('[data-testid="now-playing-widget"] a[data-testid="context-item-link"]');
    const title = titleEl ? (titleEl.innerText || "").trim() : "Unknown Track";

    const artistEl = document.querySelector('[data-testid="context-item-info-artists"]') ||
                      document.querySelector('[data-testid="track-info-artists"]') ||
                      document.querySelector('[data-testid="now-playing-widget"] [data-testid="context-item-info-subtitles"]') ||
                      document.querySelector('[data-testid="now-playing-widget"] a[href^="/artist/"]');
    const artist = artistEl ? (artistEl.innerText || "").trim() : "Unknown Artist";

    const imgEl = document.querySelector('[data-testid="cover-art-image"]') ||
                  document.querySelector('[data-testid="now-playing-widget"] img[data-testid="cover-art-image"]') ||
                  document.querySelector('[data-testid="now-playing-widget"] img');
    let artworkUrl = imgEl ? imgEl.src : "";
    
    // Filter out generic Spotify system icons/logos
    if (artworkUrl.includes("spotifycdn.com") || artworkUrl.includes("/icons/") || artworkUrl.includes("spotify.com") || artworkUrl.includes("default")) {
      artworkUrl = "";
    }

    const timeEl = document.querySelector('[data-testid="playback-position"]') ||
                   document.querySelector('.playback-bar__progress-time-elapsed');
    const currentTime = timeEl ? parseTimeToSeconds(timeEl.innerText) : 0;

    const durationEl = document.querySelector('[data-testid="playback-duration"]') ||
                        document.querySelector('.playback-bar__progress-time-total');
    const duration = durationEl ? parseTimeToSeconds(durationEl.innerText) : 0;

    return {
      title,
      artist,
      artworkUrl,
      isPlaying,
      currentTime,
      duration
    };
  } catch (e) {
    console.error("[SoundCraft] Error scraping Spotify state:", e);
    return null;
  }
}

// Get YouTube Video ID from URL
function getYouTubeVideoId() {
  const url = window.location.href;
  if (url.includes("/shorts/")) {
    const parts = url.split("/shorts/");
    if (parts.length > 1) {
      return parts[1].split(/[?#]/)[0];
    }
  }
  const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]*).*/;
  const match = url.match(regExp);
  return (match && match[2].length === 11) ? match[2] : null;
}

// YouTube Scraper
function scrapeYouTubeState() {
  try {
    const video = document.querySelector("video");
    const videoId = getYouTubeVideoId();

    if (!video || !videoId) {
      return {
        title: "No Track Playing",
        artist: "Unknown Artist",
        artworkUrl: "",
        isPlaying: false,
        currentTime: 0,
        duration: 0
      };
    }

    const isPlaying = !video.paused && !video.ended;

    const titleEl = document.querySelector("h1.ytd-watch-metadata") ||
                    document.querySelector("#title h1") ||
                    document.querySelector("meta[name='title']");
    let title = titleEl ? (titleEl.innerText || titleEl.getAttribute("content") || "").trim() : "";
    if (!title) {
      title = document.title.replace(" - YouTube", "").trim();
    }

    const artistEl = document.querySelector("#upload-info #channel-name a") ||
                      document.querySelector("ytd-channel-name a") ||
                      document.querySelector("#owner-name a") ||
                      document.querySelector(".ytd-video-owner-renderer a");
    const artist = artistEl ? (artistEl.innerText || "").trim() : "Unknown Channel";

    const artworkUrl = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
    const currentTime = Math.floor(video.currentTime || 0);
    const duration = Math.floor(video.duration || 0);

    return {
      title,
      artist,
      artworkUrl,
      isPlaying,
      currentTime,
      duration
    };
  } catch (e) {
    console.error("[SoundCraft] Error scraping YouTube state:", e);
    return null;
  }
}

// Send current state to Background Script
function sendStateUpdate(force = false) {
  const platform = getPlatform();
  let currentState = null;

  if (platform === "soundcloud") {
    currentState = scrapeSoundCloudState();
  } else if (platform === "spotify") {
    currentState = scrapeSpotifyState();
  } else if (platform === "youtube") {
    currentState = scrapeYouTubeState();
  }

  if (!currentState) return;

  const isChanged = !lastSentState ||
    currentState.title !== lastSentState.title ||
    currentState.artist !== lastSentState.artist ||
    currentState.artworkUrl !== lastSentState.artworkUrl ||
    currentState.isPlaying !== lastSentState.isPlaying ||
    Math.abs(currentState.currentTime - lastSentState.currentTime) > 1 ||
    force;

  if (isChanged) {
    chrome.runtime.sendMessage({
      type: "STATE_UPDATE",
      state: currentState,
      force: force
    });
    lastSentState = currentState;
  }
}

// Handle SoundCloud commands
function handleSoundCloudCommand(command) {
  let button = null;
  switch (command) {
    case "CMD_PLAY_PAUSE":
      button = document.querySelector(".playControl");
      break;
    case "CMD_NEXT":
      button = document.querySelector(".skipControl__next");
      break;
    case "CMD_PREV":
      button = document.querySelector(".skipControl__previous");
      break;
  }
  if (button) {
    button.click();
    setTimeout(() => sendStateUpdate(true), 150);
  }
}

// Handle Spotify commands
function handleSpotifyCommand(command) {
  let button = null;
  switch (command) {
    case "CMD_PLAY_PAUSE":
      button = document.querySelector('button[data-testid="control-button-playpause"]') ||
               document.querySelector('button[data-testid="play-button"]');
      break;
    case "CMD_NEXT":
      button = document.querySelector('button[data-testid="control-button-skip-forward"]') ||
               document.querySelector('button[data-testid="next-button"]');
      break;
    case "CMD_PREV":
      button = document.querySelector('button[data-testid="control-button-skip-back"]') ||
               document.querySelector('button[data-testid="previous-button"]');
      break;
  }
  if (button) {
    button.click();
    setTimeout(() => sendStateUpdate(true), 150);
  }
}

// Handle YouTube commands
function handleYouTubeCommand(command) {
  const video = document.querySelector("video");
  if (!video) return;

  switch (command) {
    case "CMD_PLAY_PAUSE":
      if (video.paused) {
        video.play();
      } else {
        video.pause();
      }
      break;
    case "CMD_NEXT":
      const nextButton = document.querySelector(".ytp-next-button");
      if (nextButton) {
        nextButton.click();
      }
      break;
    case "CMD_PREV":
      const prevButton = document.querySelector(".ytp-prev-button");
      if (prevButton && prevButton.style.display !== "none") {
        prevButton.click();
      } else {
        video.currentTime = 0;
      }
      break;
  }
  setTimeout(() => sendStateUpdate(true), 150);
}

// Handle Remote Commands from Background Script
function handleRemoteCommand(command) {
  console.log("[SoundCraft] Executing remote command: " + command);
  const platform = getPlatform();
  if (platform === "soundcloud") {
    handleSoundCloudCommand(command);
  } else if (platform === "spotify") {
    handleSpotifyCommand(command);
  } else if (platform === "youtube") {
    handleYouTubeCommand(command);
  }
}

// Listen for messages from Background Script
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.command === "REQUEST_UPDATE") {
    sendStateUpdate(true);
  } else if (message.command) {
    handleRemoteCommand(message.command);
  }
});

// Periodically check state (500ms intervals)
setInterval(() => {
  sendStateUpdate();
}, 500);
