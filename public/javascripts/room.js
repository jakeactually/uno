const play = ev => {
  if (document.querySelectorAll('.player-name').length < 2) {
    alert('Not enough players');
    ev.preventDefault();
  }
};

const connect = () => {
    const socket = new WebSocket(
        webSocketURLH.value.replace('ws', location.protocol == 'https:' ? 'wss' : 'ws')
    );

    socket.onmessage = ev => {
        console.log(ev);
        location.reload();
    };

    socket.onerror = ev => {
        setTimeout(connect, 1000);
    };
};

connect();
