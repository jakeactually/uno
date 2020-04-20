const play = ev {
  if (document.querySelectorAll('.player').length < 2) {
    alert('Not enough players');
    ev.preventDefault();
  }
};

const socket = new WebSocket('@routes.HomeController.roomState(roomId).webSocketURL');

socket.addEventListener('message', ev => {
    console.log(ev);
    location.reload();
});
