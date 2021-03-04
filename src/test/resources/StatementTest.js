{
  looop:
  while(i < 5) {
    if(x) {
      continue;
    }
    else if(y) {
      break looop;
    }
    else {
      i++;
    }
  }
  for(var j = 0;j<2;j++) {
    for(item in items) {
      item.x = 2;
    }
    for(var item in items) {
      foo(item);
    }
  }
  switch(x) {
    case 1:
    case 2:
      a = x;
      break;
    case 3:
      b = x;
      break;
    default:
      c = x;
      break;
    case 4:
  }
  try {
    throw e;
  } catch(e) {
    debugger;
  } finally {
    x();
  }
  return 0;
}