function a() {
  var x = function(p,q) { return p+q; };
  return x;
}

function b(m) {
  function k() {
    m();
  }
  if(1) {
    var x = function hoge() {
      return 0;
    };
  }
}