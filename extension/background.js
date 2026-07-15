let socket = null;
let activeMusicTabId = null;
let lastSentState = null;
let reconnectInterval = 3000;

function connectWebSocket() {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return;
  }

  console.log("[SoundCraft Background] Connecting to Minecraft WebSocket Server at ws://127.0.0.1:8887...");
  socket = new WebSocket("ws://127.0.0.1:8887");

  socket.onopen = () => {
    console.log("[SoundCraft Background] Connected to Minecraft Server.");
    if (activeMusicTabId !== null) {
      chrome.tabs.sendMessage(activeMusicTabId, { command: "REQUEST_UPDATE" });
    }
  };

  socket.onclose = () => {
    console.log("[SoundCraft Background] Connection closed. Retrying in 3s...");
    setTimeout(connectWebSocket, reconnectInterval);
  };

  socket.onerror = (error) => {
    console.error("[SoundCraft Background] WebSocket error:", error);
  };

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      if (data && data.command) {
        handleRemoteCommand(data.command);
      }
    } catch (e) {
      console.error("[SoundCraft Background] Error parsing incoming command:", e);
    }
  };
}

function handleRemoteCommand(command) {
  console.log("[SoundCraft Background] Received command from Minecraft: " + command);
  if (activeMusicTabId !== null) {
    chrome.tabs.sendMessage(activeMusicTabId, { command: command }, (response) => {
      if (chrome.runtime.lastError) {
        console.warn("[SoundCraft Background] Active music tab lost. Trying to find another music tab...");
        activeMusicTabId = null;
        sendToAnyMusicTab(command);
      }
    });
  } else {
    sendToAnyMusicTab(command);
  }
}

function sendToAnyMusicTab(command) {
  chrome.tabs.query({}, (tabs) => {
    const musicTab = tabs.find(tab => 
      tab.url && (
        tab.url.includes("soundcloud.com") || 
        tab.url.includes("spotify.com") || 
        tab.url.includes("youtube.com")
      )
    );
    if (musicTab) {
      console.log("[SoundCraft Background] Forwarding command to tab: " + musicTab.id);
      activeMusicTabId = musicTab.id;
      chrome.tabs.sendMessage(musicTab.id, { command: command });
    } else {
      console.warn("[SoundCraft Background] No music tabs found to send command: " + command);
    }
  });
}

// Listen for messages from content scripts
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === "STATE_UPDATE") {
    const tabId = sender.tab ? sender.tab.id : null;
    if (!tabId) return;

    const state = message.state;

    // Track active music tab: if a tab starts playing, it becomes the active music source
    if (state.isPlaying) {
      activeMusicTabId = tabId;
    }

    // Only forward to Minecraft if it is the active music tab or if no active tab has been selected yet
    if (activeMusicTabId === null || activeMusicTabId === tabId) {
      activeMusicTabId = tabId;
      
      // Auto-connect WebSocket if it is closed
      if (!socket || socket.readyState === WebSocket.CLOSED) {
        connectWebSocket();
      }

      if (socket && socket.readyState === WebSocket.OPEN) {
        const isChanged = !lastSentState ||
          state.title !== lastSentState.title ||
          state.artist !== lastSentState.artist ||
          state.artworkUrl !== lastSentState.artworkUrl ||
          state.isPlaying !== lastSentState.isPlaying ||
          Math.abs(state.currentTime - lastSentState.currentTime) > 1 ||
          message.force;

        if (isChanged) {
          socket.send(JSON.stringify(state));
          lastSentState = state;
        }
      }
    }
  }
});

// Initialize connection
connectWebSocket();
