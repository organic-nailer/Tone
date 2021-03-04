{
  while(i < 5) {
    if(x) {
      continue;
    }
    else if(y) {
      break;
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
  return 0;
}