const toggleSwitch = document.querySelector('.theme-switch input[type="checkbox"]');
const currentTheme = localStorage.getItem('theme');

if (currentTheme) {
    document.documentElement.setAttribute('data-theme', currentTheme);
  
    if (currentTheme === 'dark') {
        toggleSwitch.checked = true;
    }
}

function switchTheme(e) {
    if (e.target.checked) {
        document.documentElement.setAttribute('data-theme', 'dark');
        localStorage.setItem('theme', 'dark');
        monaco.editor.setTheme('vs-dark');
    }
    else {        
        document.documentElement.setAttribute('data-theme', 'light');
        localStorage.setItem('theme', 'light');
        monaco.editor.setTheme('vs');
    }    
}

toggleSwitch.addEventListener('change', switchTheme, false);

function saveProfile () {
    let xhr = new XMLHttpRequest();
    xhr.open('POST', window.location.href.replace('edit', 'post-profile'), false);
    xhr.setRequestHeader('Content-Type', 'text/x-yaml');
    xhr.send(document.getElementById('container').value);
    if (xhr.status != 200) {
        alert("File not found!"); 
      } else {
        alert(xhr.responseText); 
      }
}   