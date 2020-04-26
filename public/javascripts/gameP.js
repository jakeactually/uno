const roomId = roomIdH.value;
const webSocketURL = webSocketURLH.value;

let hand = [];
let center = [];

const fullFetch = () => new Promise(resolve => {
  board.innerHTML = '';
  stack = 0;
  fetchHand().then(fetchCenter).then(fetchPlayers).then(resolve);
});

const fetchHand = () => new Promise(resolve => {
  hand = [];
  fetch('/hand').then(r => r.json()).then(newHand => {
    for (const card of newHand) addCard(card);
    renderPos();
    resolve();
  });
});

const fetchCenter = () => new Promise(resolve => {
  center = [];
  fetch('/center/' + roomId).then(r => r.json()).then(newCenter => {
    for (const card of newCenter) toCenter(makeCard(card));
    const [cardId, cardName] = newCenter.pop();
    fetchBoard(cardName.match(/plus4|color/)).then(resolve);
  });
});

const handUpdate = () => new Promise(resolve => {
  fetch('/hand').then(r => r.json()).then(newHand => {
    if (hand.length < newHand.length) {
      const diff = newHand.slice(hand.length);
      diff.forEach(addCard);
      renderPos();
    }
    
    fetchPlayers().then(() => fetchBoard(false)).then(resolve);
  });
});

const centerUpdate = () => new Promise(resolve => {
  fetch('/center/' + roomId).then(r => r.json()).then(newCenter => {
    if (newCenter.length < center.length) {
      fullFetch().then(resolve);
      return;
    }
  
    const top = center[center.length - 1];
    const [newCardId, newCardName] = newCenter.pop();
  
    if (top.cardId != newCardId) {
      const card = makeCard([newCardId, newCardName]);
      toCenter(card);
      card.className += ' downflash';
  
      fetchPlayers().then(() => fetchBoard(newCardName.match(/plus4|color/))).then(resolve);
    }
  });  
});

const fetchPlayers = () => new Promise(resolve => {
  fetch('/all-players/' + roomId).then(r => r.json()).then(data => {
    if (data.filter(([name, score]) => score != 0).length <= 1)
      location.href = '/game-over/' + roomId;

    window['all-players'].innerHTML = '';
    data.forEach(makePlayer);
    resolve();
  });
});

const fetchBoard = topIsColorCard => new Promise(resolve => {
  fetch('/board-state/' + roomId).then(r => r.json()).then(board => {
    const [myTurn, drawed, color, state, count] = board;

    if (topIsColorCard) {
      message('Chosen color is ' + color);
    }

    action.innerText = 'Stand by';
    action.className = '';

    if (!myTurn) {
        resolve();
        return;
    }

    action.className = 'my-turn';

    if (state == "plus2") {
      action.innerText = 'Draw ' + count * 2;
      action.onclick = () => {
          fetch('/penalty/' + roomId, { method: 'post' });
      };
    } else if (state == "plus4") {
      action.innerText = 'Draw ' + count * 4;
      action.onclick = () => {
          fetch('/penalty/' + roomId, { method: 'post' });
      };
    } else if (state == "stop") {
      action.innerText = 'Pass';
      action.onclick = () => {
          fetch('/penalty/' + roomId, { method: 'post' });
      };
    } else if (drawed) {
      action.innerText = 'Pass';
      action.onclick = () => {
          fetch('/pass/' + roomId, { method: 'post' });
      };
    } else {
      action.innerText = 'Draw';
      action.onclick = () => {
          fetch('/draw/' + roomId, { method: 'post' }).then(handUpdate);
      };
    }

    resolve();
  });
});

const addCard = cardData => hand.push(makeCard(cardData));
const getHalf = () => Math.floor(hand.length / 2);

const makeCard = ([id, name]) => {
  const img = document.createElement('img');
  img.src = `https://jakeactually.com/uno/${name}.png`;
  img.className = 'card';
  img.ondragstart = ev => ev.preventDefault();
  img.cardId = id;
  img.cardName = name;
  board.appendChild(img);  
  return img;
};

const makePlayer = ([name, count, isCurrent]) => {
  const div = document.createElement('div');
  div.className = 'player' + (isCurrent ? ' current' : '');
  div.appendChild(document.createTextNode(name));
  div.appendChild(document.createElement('br'));
  div.appendChild(document.createTextNode(count));
  window['all-players'].appendChild(div);
  return div;
};

const renderPos = () => {
  const half = getHalf();
  const pairs =  Object.entries(hand);

  for (const [i, img] of pairs) {
    const percent = i / hand.length * 80;
    img.style.left = percent + 7 + '%';

    if (i < half) {
      img.style.top = 75 + '%';
      img.style.zIndex = i;
    }
    else if (i == half) {
      img.style.top = 70 + '%';
      img.style.zIndex = half;
    } else {
      img.style.top = 75 + '%';
      img.style.zIndex = half - i;
    }
  }
};

const toCenter = card => {
  center.push(card);

  const style = card.style;
  style.left = 40 + '%';
  const top = Math.floor(stack / 2);
  style.top = `calc(40% - ${top}px)`;
  style.zIndex = stack;
  stack++;
};

let offset = 0;
const handH = new Hammer(board);

const doPan = (str, arrF) => {
  handH.on(str, () => {
    if (offset > 10) {
      arrF();
      renderPos();
      offset = 0;
    } else {
      offset++;
    }
  });
};

doPan('panleft', () => hand.push(hand.shift()));
doPan('panright', () => hand.unshift(hand.pop()));

let stack = 0;
const handH2 = new Hammer(board);
handH2.get('swipe').set({ direction: Hammer.DIRECTION_ALL });

handH2.on('swipeup', () => {
  const half = getHalf();
  const card = hand[half];

  const body = new URLSearchParams();
  body.append('cardId', card.cardId);

  if (card.cardName.match(/plus4|color/)) {
    colorDialog().then(color => body.append('color', color)).then(() => {
      const options = { method: 'post', body: body };
      fetch('/turn/' + roomId, options).then(res => {
        if (res.ok) {
          toCenter(card);
          hand.splice(half, 1);
          renderPos();
        } else {
          res.text().then(message);
        }
      });
    });
  } else {
    const options = { method: 'post', body: body };
    fetch('/turn/' + roomId, options).then(res => {
      if (res.ok) {
        toCenter(card);
        hand.splice(half, 1);
        renderPos();
      } else {
        res.text().then(message);
      }
    });
  }
});

const colorDialog = () => new Promise(resolve => {
  window['which-color'].style.display = 'block';
  message('Pick a color');
  getColor().then(color => {
    window['which-color'].style.display = 'none';
    return color;
  }).then(resolve);
});

const colors = ['red', 'green', 'yellow', 'blue'];

const getColor = () => new Promise(resolve => {
  for (const color of colors) {
    const button = window['the-' + color];
    button.onclick = () => resolve(color);
  }
});

const message = text => new Promise(resolve => {
  banner.innerText = text;
  banner.className = 'banner-a';
  setTimeout(() => {
    banner.className = '';
    resolve();
  }, 1000);
});

const connect = () => {
    const socket = new WebSocket(webSocketURLH.value);

    socket.onmessage = ev => {
        console.log(ev);
        // await fullFetch();
        handUpdate().then(centerUpdate);
    };

    socket.onerror = ev => {
        setTimeout(connect, 1000);
    };
};

connect();

fullFetch();
