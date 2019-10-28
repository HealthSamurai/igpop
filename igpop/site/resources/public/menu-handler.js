function findInCollection(collection, item){
  for (let q = 0; q < collection.length; q++){
    if(collection[q] === item){return true;}
  }
  return false;
}

function rollUpMenu(curItem = null) {
    let dropdown = document.getElementsByClassName("dropdown-btn");
    for (let i = 0; i < dropdown.length; i++) {
        if (dropdown[i] !== curItem){
            let svg = dropdown[i].getElementsByTagName('svg')[0];
            svg.setAttribute("transform", "rotate(-90)");
            let dropdownContent = dropdown[i].nextElementSibling;
            dropdownContent.style.display = "none";
            if (findInCollection(dropdown[i].classList, "active")){
                dropdown[i].classList.toggle("active");
            }
        }
    }
}

$(document).ready(function(){
    rollUpMenu();
});

$(".btn-patient").click(function() {
    $(".body-content").load("/assets/patient.html");
    let curElement = document.getElementsByClassName("btn-patient");
    rollUpMenu(curElement[0].parentNode.previousSibling);
    return false;
});

$(".btn-organization").click(function() {
    $(".body-content").load("/assets/organization.html");
    let curElement = document.getElementsByClassName("btn-organization");
    rollUpMenu(curElement[0].parentNode.previousSibling);
    return false;
});
