const roomId = roomIdH.value;
const webSocketURL = webSocketURLH.value;

let hand = [];
let center = [];

const fullFetch = async () => {
  board.innerHTML = '';
  stack = 0;
  await fetchHand();
  await fetchCenter();
  await fetchPlayers();
};

const fetchHand = async () => {
  hand = [];
  const newHand = await fetch('/hand').then(r => r.json());
  for (const card of newHand) addCard(card);
  renderPos();
};

const fetchCenter = async () => {
  center = [];
  const newCenter  = await fetch('/center/' + roomId).then(r => r.json());
  for (const card of newCenter) toCenter(makeCard(card));
  const [cardId, cardName] = newCenter.pop();
  await fetchBoard(cardName.match(/plus4|color/));
};

const handUpdate = async () => {
  const newHand  = await fetch('/hand').then(r => r.json());

  if (hand.length < newHand.length) {
      const diff = newHand.slice(hand.length);
      diff.forEach(addCard);
      renderPos();
  }

  await fetchPlayers();
  await fetchBoard(false);
};

const centerUpdate = async () => {
  const newCenter  = await fetch('/center/' + roomId).then(r => r.json());

  if (newCenter.length < center.length) {
    await fullFetch();
    return;
  }

  const top = center[center.length - 1];
  const [newCardId, newCardName] = newCenter.pop();

  if (top.cardId != newCardId) {
    const card = makeCard([newCardId, newCardName]);
    toCenter(card);
    card.className += ' downflash';

    await fetchPlayers();
    await fetchBoard(newCardName.match(/plus4|color/));
  }
};

const fetchPlayers = async () => {
  const data  = await fetch('/all-players/' + roomId).then(r => r.json());

  if (data.filter(([name, score]) => score != 0).length <= 1)
    location.href = '/game-over/' + roomId;

  window['all-players'].innerHTML = '';
  data.forEach(makePlayer);
};

const fetchBoard = async topIsColorCard => {
  const board  = await fetch('/board-state/' + roomId).then(r => r.json());
  const [myTurn, drawed, color, state, count] = board;

  if (topIsColorCard) {
    message('Chosen color is ' + color);
  }

  if (!myTurn) return;
  action.className = 'my-turn';

  if (state == "plus2") {
    action.innerText = 'Draw ' + count * 2;
    action.onclick = async () => {
        await fetch('/penalty/' + roomId, { method: 'post' });
    };
  } else if (state == "plus4") {
    action.innerText = 'Draw ' + count * 4;
    action.onclick = async () => {
        await fetch('/penalty/' + roomId, { method: 'post' });
    };
  } else if (state == "stop") {
    action.innerText = 'Pass';
    action.onclick = async () => {
        await fetch('/penalty/' + roomId, { method: 'post' });
    };
  } else if (drawed) {
    action.innerText = 'Pass';
    action.onclick = async () => {
        await fetch('/pass/' + roomId, { method: 'post' });
    };
  } else {
    action.innerText = 'Draw';
    action.onclick = async () => {
        await fetch('/draw/' + roomId, { method: 'post' });
        await handUpdate();
    };
  }
};

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

handH2.on('swipeup', async () => {
  const half = getHalf();
  const card = hand[half];

  const body = new URLSearchParams();
  body.append('cardId', card.cardId);

  if (card.cardName.match(/plus4|color/)) {
    body.append('color', await colorDialog());
  }

  const options = { method: 'post', body: body };
  const res = await fetch('/turn/' + roomId, options);
  
  if (res.ok) {
    toCenter(card);
    hand.splice(half, 1);
    renderPos();
  } else {
    message(await res.text());
  }
});

const colorDialog = async () => {
  window['which-color'].style.display = 'block';
  message('Pick a color');
  const color = await getColor();
  window['which-color'].style.display = 'none';
  return color;
};

const colors = ['red', 'green', 'yellow', 'blue'];

const getColor = () => ({
  then: resolve => {
    for (const color of colors) {
      const button = window['the-' + color];
      button.onclick = () => resolve(color);
    }
  }
});

const message = async text => {
  banner.innerText = text;
  banner.className = 'banner-a';
  await { then: resolve => setTimeout(resolve, 1000) };
  banner.className = '';
};

const connect = () => {
    const socket = new WebSocket(webSocketURLH.value);

    socket.onmessage = async ev => {
        console.log(ev);
        // await fullFetch();
        await handUpdate();
        await centerUpdate();
    };

    socket.onerror = ev => {
        setTimeout(connect, 1000);
    };
};

connect();

fullFetch();
