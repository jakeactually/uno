const data = [
  [1, "g1"],
  [2, "b3"],
  [1, "g1"],
  [2, "b3"],
  [1, "g1"],
  [2, "b3"],
  [1, "g1"],
];

const cards = [];

const addCard = card => {
  const img = document.createElement('img');
  img.src = `https://jakeactually.com/uno/${card[1]}.png`;
  img.className = 'card';
  img.ondragstart = ev => ev.preventDefault();
  cards.push(img);
  hand.appendChild(img);
};

const renderPos = () => {
  const half = Math.floor(cards.length / 2);
  const pairs =  Object.entries(cards);

  for (const [i, img] of pairs) {
    if (i < half) {
      const percent = i / cards.length * 80;
      img.style.left = percent + 7 + '%';
      img.style.top = 80 + '%';
      img.style.zIndex = i;
    }
    else if (i == half) {
      const percent = i / cards.length * 80;
      img.style.left = percent + 7 + '%';
      img.style.top = 75 + '%';
      img.style.zIndex = i;
    } else {
      const percent = i / cards.length * 80;
      img.style.left = percent + 7 + '%';
      img.style.top = 80 + '%';
      img.style.zIndex = half - i;
    }
  }
};

/*onclick = () => {
  addCard([3, "r4"]);
  renderPos();
};*/

data.forEach(addCard);
renderPos();

let offset = 0;
const handH = new Hammer(hand);

handH.on('panleft', ev => {
  if (offset > 10) {
    cards.push(cards.shift());
    renderPos();
    offset = 0;
  } else {
    offset++;
  }
});

handH.on('panright', ev => {
  if (offset > 10) {
    cards.unshift(cards.pop());
    renderPos();
    offset = 0;
  } else {
    offset++;
  }
});

let stack = 0;
const handH2 = new Hammer(hand);
handH2.get('swipe').set({ direction: Hammer.DIRECTION_ALL });

handH2.on('swipeup', ev => {
  const half = Math.floor(cards.length / 2);
  const style = cards[half].style;
  style.left = 40 + '%';
  style.top = 40 + '%';
  style.zIndex = stack;
  stack++;
  cards.splice(half, 1);
  renderPos();
});
