let cards = [];
const roomId = roomIdH.value;
const webSocketURL = webSocketURLH.value;

const fullFetch = async () => {
  board.innerHTML = '';
  cards = [];
  stack = 0;
  await fetchHand();
  await fetchCenter();
};

const fetchHand = async () => {
  const hand = await fetch('/hand').then(r => r.json());
  for (const card of hand) addCard(card);
  renderPos();
};

const fetchCenter = async () => {
  const center  = await fetch('/center/' + roomId).then(r => r.json());
  for (const card of center) toCenter(makeCard(card));
  await fetchBoard(center.pop()[1].match(/plus4|color/));
};

const fetchBoard = async topIsColorCard => {
  const board  = await fetch('/board-state/' + roomId).then(r => r.json());
  const [myTurn, drawed, color, state, count] = board;

  window['choosen-color'].innerText = color.replace(/\w/, x => x.toUpperCase());
  if (topIsColorCard) {
    window['choosen-color'].style.display = 'block';
  } else {
    window['choosen-color'].style.display = 'none';
  }

  action.style.display = 'none';
  if (!myTurn) return;
  action.style.display = 'block';

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
        await fullFetch();
    };
  }
};

const addCard = cardData => cards.push(makeCard(cardData));
const getHalf = () => Math.floor(cards.length / 2);

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

const renderPos = () => {
  const half = getHalf();
  const pairs =  Object.entries(cards);

  for (const [i, img] of pairs) {
    const percent = i / cards.length * 80;
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
  const style = card.style;
  style.left = 40 + '%';
  style.top = `calc(40% - ${stack}px)`;
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

doPan('panleft', () => cards.push(cards.shift()));
doPan('panright', () => cards.unshift(cards.pop()));

let stack = 0;
const handH2 = new Hammer(board);
handH2.get('swipe').set({ direction: Hammer.DIRECTION_ALL });

handH2.on('swipeup', async () => {
  const half = getHalf();
  const card = cards[half];

  const body = new URLSearchParams();
  body.append('cardId', card.cardId);

  if (card.cardName.match(/plus4|color/)) {
    body.append('color', await colorDialog());
  }

  const options = { method: 'post', body: body };
  const res = await fetch('/turn/' + roomId, options);
  
  if (res.ok) {
    toCenter(card);
    cards.splice(half, 1);
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

const socket = new WebSocket(webSocketURL);
socket.addEventListener('message', async ev => {
  console.log(ev);
  await fullFetch();
});

fullFetch();
